package com.toblad.khwab.chat.engine

import com.toblad.khwab.integration.api.KhwabIntegration
import com.toblad.khwab.integration.api.request.IntegrationRequest
import com.toblad.khwab.integration.api.response.IntegrationResponse
import com.toblad.khwab.service.AccessibilityTreeMapper

class BrainChatEngine(
    private val integration: KhwabIntegration
) : ChatEngine {

    override suspend fun process(
        prompt: String
    ): IntegrationResponse {

        // Capture the live screen snapshot so Core can resolve UI element targets.
        // Returns null when the Accessibility Service is not connected — Core handles
        // null screenContext gracefully (falls back to raw entity text).
        val screenSnapshot = AccessibilityTreeMapper.capture()

        return integration.process(
            IntegrationRequest(
                input = prompt,
                screenContext = screenSnapshot
            )
        )
    }
}