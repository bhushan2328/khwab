package com.toblad.khwab.background

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.toblad.khwab.db.KhwabDatabase
import com.toblad.khwab.db.repository.RoomTemporaryKnowledgeRepository
import com.toblad.khwab.integration.llm.LLMService
import com.toblad.khwab.integration.openai.LLMKnowledgeExtractor
import com.toblad.khwab.integration.openai.OpenAIClient
import com.toblad.khwab.integration.openai.OpenAIConfig
import com.toblad.khwab.integration.openai.RelatedPromptBuilder
import com.toblad.khwab.security.ApiKeyStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import java.util.UUID

/**
 * WorkManager [CoroutineWorker] that acquires knowledge from ChatGPT in the background.
 *
 * Enqueue via [enqueue]. Deduplication is enforced by unique work name so the same
 * query is never processed twice concurrently.
 *
 * On success the result is stored in [RoomTemporaryKnowledgeRepository] (30-day TTL).
 * The UI observes Room via Flow and updates when data arrives.
 */
class KnowledgeAcquisitionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "KnowledgeAcquisition"
        const val KEY_QUERY = "query"
        const val KEY_TTL_DAYS = "ttl_days"
        /** Output key: primary answer text placed in Result.success() outputData. */
        const val KEY_ANSWER = "answer"
        private const val TIMEOUT_MS = 30_000L

        /**
         * Enqueue a one-time knowledge acquisition job for [query].
         *
         * Uses [ExistingWorkPolicy.KEEP] so a second request for the same query
         * is silently dropped if one is already pending or running.
         *
         * Returns the [UUID] of the enqueued work request so the caller can
         * observe [WorkInfo] and retrieve the answer from [KEY_ANSWER].
         */
        fun enqueue(context: Context, query: String, ttlDays: Int = 30): UUID {
            val normKey = query.trim().lowercase()

            val request = OneTimeWorkRequestBuilder<KnowledgeAcquisitionWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    workDataOf(
                        KEY_QUERY to normKey,
                        KEY_TTL_DAYS to ttlDays
                    )
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "knowledge_acquisition_$normKey",
                    androidx.work.ExistingWorkPolicy.KEEP,
                    request
                )

            Log.d(TAG, "Enqueued acquisition for: $normKey (id=${request.id})")
            return request.id
        }
    }

    override suspend fun doWork(): Result = coroutineScope {
        val query = inputData.getString(KEY_QUERY) ?: return@coroutineScope Result.failure()
        val ttlDays = inputData.getInt(KEY_TTL_DAYS, 30)

        Log.d(TAG, "Starting knowledge acquisition: $query")

        val apiKey = ApiKeyStore(applicationContext).getApiKey()
            ?: run {
                Log.w(TAG, "No API key configured — skipping acquisition")
                return@coroutineScope Result.failure()
            }

        val config = OpenAIConfig(apiKey = apiKey)
        val client = OpenAIClient(config)
        val llmService = LLMService(client)
        val promptBuilder = RelatedPromptBuilder()
        val extractor = LLMKnowledgeExtractor()
        val repo = RoomTemporaryKnowledgeRepository(
            KhwabDatabase.getInstance(applicationContext).temporaryKnowledgeDao()
        )

        return@coroutineScope try {
            var primaryAnswer: String? = null

            withTimeout(TIMEOUT_MS) {
                val prompts = promptBuilder.buildSet(query)

                // Run all three prompts in parallel
                val primaryDeferred = async {
                    llmService.generate(prompts.primaryPrompt, config.model)
                }
                val contextDeferred = async {
                    llmService.generate(prompts.contextPrompt, config.model)
                }
                val relatedDeferred = async {
                    llmService.generate(prompts.relatedPrompt, config.model)
                }

                val primary = primaryDeferred.await()
                val context = contextDeferred.await()
                val related = relatedDeferred.await()

                var savedCount = 0

                primary?.let { r ->
                    extractor.extract(query, r)?.let { record ->
                        repo.save(record.key, record.value, ttlDays, record.confidence)
                        primaryAnswer = record.value   // capture for output
                        savedCount++
                    }
                }
                context?.let { r ->
                    extractor.extract(query, r, "::context")?.let { record ->
                        repo.save(record.key, record.value, ttlDays, record.confidence)
                        savedCount++
                    }
                }
                related?.let { r ->
                    extractor.extract(query, r, "::related")?.let { record ->
                        repo.save(record.key, record.value, ttlDays, record.confidence)
                        savedCount++
                    }
                }

                Log.d(TAG, "Acquisition complete: $savedCount records saved for '$query'")
                client.close()
            }

            val answer = primaryAnswer
            if (answer != null) {
                Result.success(workDataOf(KEY_ANSWER to answer))
            } else {
                Log.w(TAG, "No extractable answer for '$query'")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Acquisition failed for '$query': ${e.message}")
            try { client.close() } catch (_: Exception) {}
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
