package com.toblad.khwab.core.memory.session.internal

import com.toblad.khwab.core.memory.model.Memory
import com.toblad.khwab.core.memory.session.SessionMemory

class RamSessionMemory : SessionMemory {

    private val memories = mutableMapOf<String, Memory>()

    override fun set(memory: Memory) {
        memories[memory.subject.lowercase()] = memory
    }

    override fun get(subject: String): Memory? {
        return memories[subject.lowercase()]
    }

    override fun remove(subject: String) {
        memories.remove(subject.lowercase())
    }

    override fun clear() {
        memories.clear()
    }

    override fun all(): List<Memory> {
        return memories.values.toList()
    }
}
