package com.toblad.khwab.di

import android.content.Context
import com.toblad.khwab.chat.engine.BrainChatEngine
import com.toblad.khwab.chat.engine.ChatEngine
import com.toblad.khwab.db.KhwabDatabase
import com.toblad.khwab.db.repository.RoomPermanentMemory
import com.toblad.khwab.db.repository.RoomTemporaryKnowledgeRepository
import com.toblad.khwab.integration.api.KhwabIntegration
import com.toblad.khwab.integration.bridge.core.DefaultCoreBridge
import com.toblad.khwab.integration.internal.DefaultKhwabIntegration
import com.toblad.khwab.integration.openai.OpenAIConfig
import com.toblad.khwab.security.ApiKeyStore

/**
 * Application-scoped dependency provider.
 *
 * Call [init] once from Application.onCreate (or lazily before first use).
 */
object KhwabProvider {

    @Volatile
    private var _chatEngine: ChatEngine? = null

    @Volatile
    private var _integration: KhwabIntegration? = null

    @Volatile
    private var _apiKeyStore: ApiKeyStore? = null

    fun init(context: Context) {
        if (_chatEngine != null) return
        synchronized(this) {
            if (_chatEngine != null) return

            val appContext = context.applicationContext
            val db = KhwabDatabase.getInstance(appContext)

            val permanentMemory = RoomPermanentMemory(db.permanentMemoryDao())
            val temporaryKnowledge = RoomTemporaryKnowledgeRepository(db.temporaryKnowledgeDao())

            val apiKeyStore = ApiKeyStore(appContext).also { _apiKeyStore = it }
            val openAIConfig = apiKeyStore.getApiKey()?.let { key ->
                OpenAIConfig(apiKey = key)
            }

            val bridge = DefaultCoreBridge(
                permanentMemory = permanentMemory,
                temporaryKnowledge = temporaryKnowledge,
                openAIConfig = openAIConfig
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

    /** The fully-wired integration instance (Room + OpenAI). Use in VoiceService. */
    val integration: KhwabIntegration
        get() = _integration
            ?: error("KhwabProvider not initialized. Call KhwabProvider.init(context) first.")

    val apiKeyStore: ApiKeyStore
        get() = _apiKeyStore
            ?: error("KhwabProvider not initialized. Call KhwabProvider.init(context) first.")

    /**
     * Re-initialise the engine after the user saves a new API key.
     * Clears the cached engine so the next call to [chatEngine] rebuilds it.
     */
    fun reinitialize(context: Context) {
        synchronized(this) {
            _chatEngine = null
            _integration = null
            _apiKeyStore = null
        }
        init(context)
    }
}
