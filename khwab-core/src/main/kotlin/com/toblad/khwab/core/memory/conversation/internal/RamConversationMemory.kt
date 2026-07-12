package com.toblad.khwab.core.memory.conversation.internal

import com.toblad.khwab.core.memory.conversation.ConversationMemory
import com.toblad.khwab.core.memory.model.Memory

class RamConversationMemory : ConversationMemory {

    private val memories = mutableMapOf<String, Memory>()

    override fun remember(memory: Memory) {
        memories[memory.subject.lowercase()] = memory
    }

    override fun recall(subject: String): Memory? {
        return memories[subject.lowercase()]
    }

    override fun recallAll(): List<Memory> {
        return memories.values.toList()
    }

    override fun forget(subject: String) {
        memories.remove(subject.lowercase())
    }

    override fun clear() {
        memories.clear()
    }

    override fun promote(subject: String): Memory? {
        return memories[subject.lowercase()]
    }
}
