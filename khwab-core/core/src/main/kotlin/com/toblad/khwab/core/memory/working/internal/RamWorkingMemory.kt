package com.toblad.khwab.core.memory.working.internal

import com.toblad.khwab.core.memory.model.Memory
import com.toblad.khwab.core.memory.working.WorkingMemory

class RamWorkingMemory : WorkingMemory {

    private val memories = mutableMapOf<String, Memory>()

    override fun put(memory: Memory) {
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
}
