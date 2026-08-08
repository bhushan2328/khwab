package com.toblad.khwab.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AssistantStateManager {

    private val _state = MutableStateFlow(AssistantState.STOPPED)

    /** Observe this from any coroutine scope or Compose via collectAsState(). */
    val stateFlow: StateFlow<AssistantState> = _state.asStateFlow()

    /** Current value — safe to read from any thread. */
    val state: AssistantState get() = _state.value

    fun updateState(newState: AssistantState) {
        _state.value = newState
    }
}
