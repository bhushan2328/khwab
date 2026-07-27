package com.toblad.khwab.chat.engine

import com.toblad.khwab.integration.api.response.IntegrationResponse

interface ChatEngine {

    suspend fun process(
        prompt: String
    ): IntegrationResponse
}