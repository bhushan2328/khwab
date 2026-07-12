package com.toblad.khwab.core.conversation

import com.toblad.khwab.core.context.ConversationContext
import java.util.UUID

/**
 * Represents one active conversation.
 */
data class ConversationSession(

    val id: String = UUID.randomUUID().toString(),

    val context: ConversationContext = ConversationContext(),

    val history: ConversationHistory = ConversationHistory(),

    val startedAt: Long = System.currentTimeMillis(),

    var lastActivity: Long = System.currentTimeMillis()

) {

    fun touch() {
        lastActivity = System.currentTimeMillis()
    }

    fun record(message: String) {
        history.add(message)
        touch()
    }
}
