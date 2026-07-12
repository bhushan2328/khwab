package com.toblad.khwab.core.memory.working.internal

import com.toblad.khwab.core.memory.model.Memory
import com.toblad.khwab.core.memory.working.WorkingMemory

class RamWorkingMemory : WorkingMemory {

    private val memories = mutableMapOf<String, Memory>()

    override fun put(memory: Memory) {
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
}
