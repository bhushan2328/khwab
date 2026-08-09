package com.toblad.khwab.chat.engine

import com.toblad.khwab.integration.api.response.IntegrationResponse
import com.toblad.khwab.integration.model.task.DynamicExecutionResponse
import com.toblad.khwab.integration.model.task.TaskState

interface ChatEngine {

    suspend fun process(
        prompt: String
    ): IntegrationResponse

    /**
     * Replans the next action for an in-progress dynamic task based on the
     * fresh screen snapshot in [state].
     */
    suspend fun replan(state: TaskState): DynamicExecutionResponse
}