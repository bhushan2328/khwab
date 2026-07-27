package com.toblad.khwab.chat.model

data class ChatMessage(
    val id: Long,
    val text: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT
)

enum class Sender {
    USER,
    KHWAB
}

enum class MessageStatus {
    SENDING,
    SENT,
    ERROR
}