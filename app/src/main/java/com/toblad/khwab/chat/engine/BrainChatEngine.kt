package com.toblad.khwab.chat.engine

import com.toblad.khwab.integration.api.KhwabIntegration
import com.toblad.khwab.integration.api.request.IntegrationRequest
import com.toblad.khwab.integration.api.response.IntegrationResponse

class BrainChatEngine(
    private val integration: KhwabIntegration
) : ChatEngine {

    override suspend fun process(
        prompt: String
    ): IntegrationResponse {

        return integration.process(
            IntegrationRequest(
                input = prompt
            )
        )
    }
}