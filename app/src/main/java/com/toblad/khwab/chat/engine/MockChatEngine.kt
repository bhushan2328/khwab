package com.toblad.khwab.chat.engine

import com.toblad.khwab.integration.api.request.IntegrationRequest
import com.toblad.khwab.integration.api.response.IntegrationResponse
import com.toblad.khwab.integration.model.error.IntegrationError
import com.toblad.khwab.integration.model.execution.ExecutionPlan
import com.toblad.khwab.integration.model.metadata.IntegrationMetadata

class MockChatEngine : ChatEngine {

    override suspend fun process(
        prompt: String
    ): IntegrationResponse {

        return IntegrationResponse(
            success = true,
            executionPlan = ExecutionPlan(
                action = "CHAT_REPLY",
                response = "Sure! I received \"$prompt\". This is a mock response from Khwab."
            ),
            metadata = IntegrationMetadata(
                processingTimeMillis = 15,
                timestamp = System.currentTimeMillis()
            ),
            error = null
        )
    }
}