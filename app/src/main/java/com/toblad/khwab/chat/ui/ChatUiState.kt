package com.toblad.khwab.chat.ui

import com.toblad.khwab.chat.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isTyping: Boolean = false,
    val isListening: Boolean = false
)
