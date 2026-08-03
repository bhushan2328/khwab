package com.toblad.khwab.chat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toblad.khwab.chat.engine.ChatEngine
import com.toblad.khwab.chat.model.ChatMessage
import com.toblad.khwab.chat.model.MessageState
import com.toblad.khwab.chat.model.MessageStatus
import com.toblad.khwab.chat.model.Sender
import com.toblad.khwab.di.KhwabProvider
import com.toblad.khwab.executor.AndroidExecutionEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    application: Application,
    private val chatEngine: ChatEngine = KhwabProvider.chatEngine
) : AndroidViewModel(application) {

    private val executionEngine = AndroidExecutionEngine(
        application.applicationContext
    )

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
                response.message ?: "I'm not sure how to respond."
            } else {
                response.error?.message ?: "Something went wrong."
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
                    executionEngine.execute(plan)
                }
            }
        }
    }
}