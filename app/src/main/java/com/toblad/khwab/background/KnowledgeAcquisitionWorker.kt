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
import com.toblad.khwab.BuildConfig
import com.toblad.khwab.db.KhwabDatabase
import com.toblad.khwab.db.repository.RoomPermanentMemory
import com.toblad.khwab.db.repository.RoomTemporaryKnowledgeRepository
import com.toblad.khwab.integration.llm.LLMService
import com.toblad.khwab.integration.openai.FallbackLLMClient
import com.toblad.khwab.integration.openai.GeminiClient
import com.toblad.khwab.integration.openai.GeminiConfig
import com.toblad.khwab.integration.openai.LLMKnowledgeExtractor
import com.toblad.khwab.integration.openai.OpenRouterClient
import com.toblad.khwab.integration.openai.OpenRouterConfig
import com.toblad.khwab.integration.openai.RelatedPromptBuilder
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
        private const val TIMEOUT_MS = 60_000L

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

        val db = KhwabDatabase.getInstance(applicationContext)
        val geminiClient = GeminiClient(GeminiConfig(apiKey = BuildConfig.GEMINI_API_KEY))
        val openRouterClient = OpenRouterClient(OpenRouterConfig(apiKey = BuildConfig.OPENROUTER_API_KEY))
        val client = FallbackLLMClient(primary = geminiClient, fallback = openRouterClient)
        val llmService = LLMService(client)
        val promptBuilder = RelatedPromptBuilder()
        val extractor = LLMKnowledgeExtractor()
        val repo = RoomTemporaryKnowledgeRepository(db.temporaryKnowledgeDao())

        // Gather relevant memory context to enrich the prompts
        val memoryContext = buildMemoryContext(
            query = query,
            permanentMemory = RoomPermanentMemory(db.permanentMemoryDao()),
            temporaryRepo = repo
        )

        return@coroutineScope try {
            var primaryAnswer: String? = null

            withTimeout(TIMEOUT_MS) {
                val prompts = promptBuilder.buildSet(query, memoryContext)

                // Fire the primary prompt first — it's the one shown to the user.
                // Only burn extra quota on context/related if the primary succeeds,
                // to preserve free-tier limits (Gemini: 15 req/min, 1500/day).
                val primary = llmService.generate(prompts.primaryPrompt, "gemini-2.0-flash")
                var savedCount = 0

                primary?.let { r ->
                    extractor.extract(query, r)?.let { record ->
                        repo.save(record.key, record.value, ttlDays, record.confidence)
                        primaryAnswer = record.value
                        savedCount++
                    }
                }

                // Only fire enrichment calls if primary gave a usable answer
                if (primaryAnswer != null) {
                    val contextDeferred = async {
                        llmService.generate(prompts.contextPrompt, "gemini-2.0-flash")
                    }
                    val relatedDeferred = async {
                        llmService.generate(prompts.relatedPrompt, "gemini-2.0-flash")
                    }
                    contextDeferred.await()?.let { r ->
                        extractor.extract(query, r, "::context")?.let { record ->
                            repo.save(record.key, record.value, ttlDays, record.confidence)
                            savedCount++
                        }
                    }
                    relatedDeferred.await()?.let { r ->
                        extractor.extract(query, r, "::related")?.let { record ->
                            repo.save(record.key, record.value, ttlDays, record.confidence)
                            savedCount++
                        }
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

    /**
     * Assembles a short memory-context block from permanent memory (all records) and
     * the best temporary knowledge match for [query].
     *
     * Only includes entries whose subject/key is related to the query (substring match).
     * Result is a plain-text block suitable for inclusion in an LLM prompt.
     */
    private suspend fun buildMemoryContext(
        query: String,
        permanentMemory: RoomPermanentMemory,
        temporaryRepo: RoomTemporaryKnowledgeRepository
    ): String {
        val norm = query.trim().lowercase()
        val sb = StringBuilder()

        // Permanent memory: include records whose subject is mentioned in the query
        permanentMemory.all()
            .filter { mem ->
                norm.contains(mem.subject.lowercase()) ||
                mem.subject.lowercase().split(" ").any { norm.contains(it) && it.length > 3 }
            }
            .take(5)
            .forEach { mem -> sb.appendLine("- ${mem.subject}: ${mem.value}") }

        // Best temporary match
        val tempResult = temporaryRepo.search(query)
        if (tempResult.tier != com.toblad.khwab.core.knowledge.KnowledgeTier.NONE) {
            tempResult.record?.let { rec ->
                sb.appendLine("- (previously learned) ${rec.key}: ${rec.value.take(200)}")
            }
        }

        return sb.toString().trim()
    }
}
