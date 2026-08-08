package com.toblad.khwab.background

import android.content.Context
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
import com.toblad.khwab.integration.bridge.core.GEMINI_MODEL
import com.toblad.khwab.integration.llm.LLMService
import com.toblad.khwab.integration.llm.providers.FallbackLLMClient
import com.toblad.khwab.integration.llm.providers.GeminiClient
import com.toblad.khwab.integration.llm.providers.GeminiConfig
import com.toblad.khwab.integration.llm.providers.LLMKnowledgeExtractor
import com.toblad.khwab.integration.llm.providers.OpenRouterClient
import com.toblad.khwab.integration.llm.providers.OpenRouterConfig
import com.toblad.khwab.integration.llm.providers.RelatedPromptBuilder
import com.toblad.khwab.di.KhwabProvider
import com.toblad.khwab.logging.LogModule
import com.toblad.khwab.logging.Logger
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
        const val KEY_ANSWER = "answer"
        /** Conversation history stored as a flat string array: [role0, msg0, role1, msg1, …] */
        private const val KEY_HISTORY = "history"
        private const val TIMEOUT_MS = 60_000L

        /**
         * Enqueue a one-time knowledge acquisition job for [query].
         *
         * [history] is the last N conversation turns as (role, text) pairs.
         * It is serialised into a flat String array for WorkManager input data.
         */
        fun enqueue(
            context: Context,
            query: String,
            ttlDays: Int = 30,
            history: List<Pair<String, String>> = emptyList()
        ): UUID {
            val normKey = query.trim().lowercase()
            // Flatten pairs → ["User","hello","Khwab","hi there",…]
            val historyFlat = history.flatMap { (role, msg) -> listOf(role, msg) }.toTypedArray()

            val request = OneTimeWorkRequestBuilder<KnowledgeAcquisitionWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    workDataOf(
                        KEY_QUERY to normKey,
                        KEY_TTL_DAYS to ttlDays,
                        KEY_HISTORY to historyFlat
                    )
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "knowledge_acquisition_$normKey",
                    androidx.work.ExistingWorkPolicy.KEEP,
                    request
                )

            Logger.debug(LogModule.AI, "Enqueued acquisition for: $normKey (id=${request.id})")
            return request.id
        }
    }

    override suspend fun doWork(): Result = coroutineScope {
        val query = inputData.getString(KEY_QUERY) ?: return@coroutineScope Result.failure()
        val ttlDays = inputData.getInt(KEY_TTL_DAYS, 30)

        // Reconstruct conversation history from flat string array
        val historyFlat = inputData.getStringArray(KEY_HISTORY) ?: emptyArray()
        val conversationHistory = historyFlat
            .toList()
            .chunked(2)
            .mapNotNull { chunk -> if (chunk.size == 2) chunk[0] to chunk[1] else null }

        Logger.debug(LogModule.AI, "Starting knowledge acquisition: $query (history=${conversationHistory.size} turns)")

        val db = KhwabDatabase.getInstance(applicationContext)

        // Reuse the shared FallbackLLMClient from KhwabProvider — avoids creating
        // duplicate HttpClient thread pools for every worker run (Fix 7).
        // Falls back to a local instance if provider is not yet initialised.
        val client: FallbackLLMClient = try {
            KhwabProvider.init(applicationContext)
            KhwabProvider.llmClient ?: FallbackLLMClient(
                primary = GeminiClient(GeminiConfig(apiKey = BuildConfig.GEMINI_API_KEY)),
                fallback = OpenRouterClient(OpenRouterConfig(apiKey = BuildConfig.OPENROUTER_API_KEY))
            )
        } catch (_: Exception) {
            FallbackLLMClient(
                primary = GeminiClient(GeminiConfig(apiKey = BuildConfig.GEMINI_API_KEY)),
                fallback = OpenRouterClient(OpenRouterConfig(apiKey = BuildConfig.OPENROUTER_API_KEY))
            )
        }
        val llmService = LLMService(client)
        val promptBuilder = RelatedPromptBuilder()
        val extractor = LLMKnowledgeExtractor()

        // Reuse shared repositories from KhwabProvider when available; fall back to fresh
        // instances only if the provider has not yet been initialised (rare edge case).
        val repo = KhwabProvider.temporaryKnowledge
            ?: RoomTemporaryKnowledgeRepository(db.temporaryKnowledgeDao())
        val permMemory = KhwabProvider.permanentMemory
            ?: RoomPermanentMemory(db.permanentMemoryDao())

        // Gather relevant memory context to enrich the prompts
        val memoryContext = buildMemoryContext(
            query = query,
            permanentMemory = permMemory,
            temporaryRepo = repo
        )

        return@coroutineScope try {
            var primaryAnswer: String? = null

            withTimeout(TIMEOUT_MS) {
                val prompts = promptBuilder.buildSet(query, memoryContext, conversationHistory)

                // Fire the primary prompt first — it's the one shown to the user.
                // Only burn extra quota on context/related if the primary succeeds,
                // to preserve free-tier limits (Gemini: 15 req/min, 1500/day).
                val primary = llmService.generate(prompts.primaryPrompt, GEMINI_MODEL)
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
                        llmService.generate(prompts.contextPrompt, GEMINI_MODEL)
                    }
                    val relatedDeferred = async {
                        llmService.generate(prompts.relatedPrompt, GEMINI_MODEL)
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

                Logger.debug(LogModule.AI, "Acquisition complete: $savedCount records saved for '$query'")
                // Only close the client if this worker owns it (not the shared provider instance).
                if (client !== KhwabProvider.llmClient) {
                    try { client.close() } catch (_: Exception) {}
                }
            }

            val answer = primaryAnswer
            if (answer != null) {
                Result.success(workDataOf(KEY_ANSWER to answer))
            } else {
                Logger.warning(LogModule.AI, "No extractable answer for '$query'")
                Result.failure()
            }
        } catch (e: Exception) {
            Logger.error(LogModule.AI, "Acquisition failed for '$query': ${e.message}")
            // Only close if it's NOT the shared provider client
            if (client !== KhwabProvider.llmClient) {
                try { client.close() } catch (_: Exception) {}
            }
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

