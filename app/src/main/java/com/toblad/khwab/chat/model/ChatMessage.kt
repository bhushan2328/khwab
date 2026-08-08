package com.toblad.khwab.chat.model

data class ChatMessage(
    val id: Long,
    val text: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val state: MessageState = MessageState.COMPLETE,
    /**
     * True only for brand-new Khwab messages that should be revealed with a
     * typewriter animation. The UI clears this flag once the animation finishes,
     * so it is never persisted and is always false when loaded from Room.
     */
    val isNew: Boolean = false
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

enum class MessageState {
    STREAMING,
    COMPLETE,
    SPEAKING
}