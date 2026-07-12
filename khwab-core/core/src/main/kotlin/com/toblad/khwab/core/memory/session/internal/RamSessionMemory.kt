package com.toblad.khwab.core.memory.session.internal

import com.toblad.khwab.core.memory.model.Memory
import com.toblad.khwab.core.memory.session.SessionMemory

class RamSessionMemory : SessionMemory {

    private val memories = mutableMapOf<String, Memory>()

    override fun set(memory: Memory) {
        memories[memory.subject.lowercase()] = memory
    }

    override fun get(key: String): Memory? {
        return memories[key.lowercase()]
    }

    override fun remove(key: String) {
        memories.remove(key.lowercase())
    }

    override fun clear() {
        memories.clear()
    }

    override fun all(): List<Memory> {
        return memories.values.toList()
    }
}
