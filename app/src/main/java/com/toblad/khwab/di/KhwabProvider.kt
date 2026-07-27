package com.toblad.khwab.di

import com.toblad.khwab.chat.engine.BrainChatEngine
import com.toblad.khwab.chat.engine.ChatEngine
import com.toblad.khwab.integration.bridge.core.DefaultCoreBridge
import com.toblad.khwab.integration.internal.DefaultKhwabIntegration

object KhwabProvider {

    val chatEngine: ChatEngine by lazy {

        val integration = DefaultKhwabIntegration(
            coreBridge = DefaultCoreBridge()
        )

        integration.initialize()

        BrainChatEngine(integration)
    }
}
