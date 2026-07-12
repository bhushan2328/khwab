package com.toblad.khwab.core.memory.working

import com.toblad.khwab.core.memory.model.Memory

interface WorkingMemory {

    fun put(memory: Memory)

    fun get(key: String): Memory?

    fun remove(key: String)

    fun clear()
}
