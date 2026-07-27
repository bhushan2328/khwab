package com.toblad.khwab.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toblad.khwab.chat.engine.ChatEngine
import com.toblad.khwab.di.KhwabProvider
import com.toblad.khwab.chat.model.ChatMessage
import com.toblad.khwab.chat.model.MessageState
import com.toblad.khwab.chat.model.MessageStatus
import com.toblad.khwab.chat.model.Sender
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatEngine: ChatEngine = KhwabProvider.chatEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    id = 1L,
                    text = "Hello Mr. Bhushan! I'm Khwab. How can I help you today?",
                    sender = Sender.KHWAB
                )
            )
        )
    )

    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputChanged(text: String) {
        _uiState.update {
            it.copy(input = text)
        }
    }

    fun sendMessage() {

        val input = uiState.value.input.trim()

        if (input.isBlank()) return

        val userMessage = ChatMessage(
            id = System.currentTimeMillis(),
            text = input,
            sender = Sender.USER,
            status = MessageStatus.SENT,
            state = MessageState.COMPLETE
        )

        _uiState.update {
            it.copy(
                input = "",
                isTyping = true,
                messages = it.messages + userMessage
            )
        }

        viewModelScope.launch {

            delay(300)

            val response = chatEngine.process(input)

            val replyText = if (response.success) {
                response.executionPlan?.response
                    ?: "Done."
            } else {
                response.error?.message
                    ?: "Something went wrong."
            }

            _uiState.update { state ->

                state.copy(
                    isTyping = false,
                    messages = state.messages + ChatMessage(
                        id = System.currentTimeMillis(),
                        text = replyText,
                        sender = Sender.KHWAB,
                        status = MessageStatus.SENT,
                        state = MessageState.COMPLETE
                    )
                )
            }

            if (response.success) {

                response.executionPlan?.let { plan ->

                    when (plan.action) {

                        "CHAT_REPLY" -> {
                            // Conversation only.
                        }

                        "OPEN_APP" -> {
                            // TODO: Launch app.
                        }

                        "SET_ALARM" -> {
                            // TODO: Set alarm.
                        }

                        "OPEN_SETTINGS" -> {
                            // TODO: Open Android settings.
                        }

                        else -> {
                            // TODO: Handle future actions.
                        }
                    }
                }
            }
        }
    }
}
