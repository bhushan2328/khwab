package com.toblad.khwab.core.memory.conversation

import com.toblad.khwab.core.memory.model.Memory

interface ConversationMemory {

    fun remember(memory: Memory)

    fun recall(key: String): Memory?

    fun recallAll(): List<Memory>

    fun forget(key: String)

    fun clear()

    fun promote(key: String): Memory?
}
