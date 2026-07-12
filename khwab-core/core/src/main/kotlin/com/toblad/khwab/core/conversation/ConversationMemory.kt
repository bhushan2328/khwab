package com.toblad.khwab.core.conversation

class ConversationMemory {

    private var last: ConversationContext? = null

    fun remember(context: ConversationContext) {
        last = context
    }

    fun recall(): ConversationContext? = last

    fun clear() {
        last = null
    }
}
