package com.toblad.khwab.di

import android.content.Context
import com.toblad.khwab.BuildConfig
import com.toblad.khwab.chat.engine.BrainChatEngine
import com.toblad.khwab.chat.engine.ChatEngine
import com.toblad.khwab.db.KhwabDatabase
import com.toblad.khwab.db.repository.RoomPermanentMemory
import com.toblad.khwab.db.repository.RoomTemporaryKnowledgeRepository
import com.toblad.khwab.integration.api.KhwabIntegration
import com.toblad.khwab.integration.bridge.core.DefaultCoreBridge
import com.toblad.khwab.integration.internal.DefaultKhwabIntegration
import com.toblad.khwab.integration.openai.FallbackLLMClient
import com.toblad.khwab.integration.openai.GeminiClient
import com.toblad.khwab.integration.openai.GeminiConfig
import com.toblad.khwab.integration.openai.OpenRouterClient
import com.toblad.khwab.integration.openai.OpenRouterConfig

/**
 * Application-scoped dependency provider.
 *
 * Call [init] once from Application.onCreate (or lazily before first use).
 * API keys are baked in at build time via BuildConfig — no runtime key entry needed.
 */
object KhwabProvider {

    @Volatile
    private var _chatEngine: ChatEngine? = null

    @Volatile
    private var _integration: KhwabIntegration? = null

    fun init(context: Context) {
        if (_chatEngine != null) return
        synchronized(this) {
            if (_chatEngine != null) return

            val appContext = context.applicationContext
            val db = KhwabDatabase.getInstance(appContext)

            val permanentMemory = RoomPermanentMemory(db.permanentMemoryDao())
            val temporaryKnowledge = RoomTemporaryKnowledgeRepository(db.temporaryKnowledgeDao())

            val geminiClient = GeminiClient(GeminiConfig(apiKey = BuildConfig.GEMINI_API_KEY))
            val openRouterClient = OpenRouterClient(OpenRouterConfig(apiKey = BuildConfig.OPENROUTER_API_KEY))
            val llmClient = FallbackLLMClient(primary = geminiClient, fallback = openRouterClient)

            val bridge = DefaultCoreBridge(
                permanentMemory = permanentMemory,
                temporaryKnowledge = temporaryKnowledge,
                llmClient = llmClient
            )

            val integration = DefaultKhwabIntegration(bridge)
                .also { it.initialize() }
                .also { _integration = it }
            _chatEngine = BrainChatEngine(integration)
        }
    }

    val chatEngine: ChatEngine
        get() = _chatEngine
            ?: error("KhwabProvider not initialized. Call KhwabProvider.init(context) first.")

    /** The fully-wired integration instance (Room + LLM). Use in VoiceService. */
    val integration: KhwabIntegration
        get() = _integration
            ?: error("KhwabProvider not initialized. Call KhwabProvider.init(context) first.")
}
