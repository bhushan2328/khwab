package com.toblad.khwab.core.memory.session

import com.toblad.khwab.core.memory.model.Memory

interface SessionMemory {

    fun set(memory: Memory)

    fun get(key: String): Memory?

    fun remove(key: String)

    fun clear()

    fun all(): List<Memory>
}
