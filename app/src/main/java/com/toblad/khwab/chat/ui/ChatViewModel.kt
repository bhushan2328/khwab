package com.toblad.khwab.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class ChatViewModel : ViewModel() {

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
            status = MessageStatus.SENT
        )

        _uiState.update {
            it.copy(
                input = "",
                isTyping = true,
                messages = it.messages + userMessage
            )
        }

        viewModelScope.launch {

            delay(700)

            val replyId = System.currentTimeMillis()

            _uiState.update {
                it.copy(
                    isTyping = false,
                    messages = it.messages + ChatMessage(
                        id = replyId,
                        text = "",
                        sender = Sender.KHWAB,
                        state = MessageState.STREAMING
                    )
                )
            }

            val fullReply =
                "Sure! I received \"$input\". This response is streaming word by word from Khwab."

            val words = fullReply.split(" ")

            var streamedText = ""

            words.forEachIndexed { index, word ->

                streamedText =
                    if (index == 0) word
                    else "$streamedText $word"

                _uiState.update { state ->

                    state.copy(
                        messages = state.messages.map { message ->

                            if (message.id == replyId) {
                                message.copy(
                                    text = streamedText,
                                    state = MessageState.STREAMING
                                )
                            } else {
