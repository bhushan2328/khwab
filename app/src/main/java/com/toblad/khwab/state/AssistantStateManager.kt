package com.toblad.khwab.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AssistantStateManager {

    var state by mutableStateOf(AssistantState.STOPPED)
        private set

    fun updateState(newState: AssistantState) {
        state = newState
    }

}