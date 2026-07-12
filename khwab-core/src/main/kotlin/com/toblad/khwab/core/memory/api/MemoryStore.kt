package com.toblad.khwab.core.memory.api

import com.toblad.khwab.core.memory.model.Memory
import com.toblad.khwab.core.memory.model.MemoryId

interface MemoryStore {

    fun save(memory: Memory)

    fun findById(id: MemoryId): Memory?

    fun findAll(): List<Memory>

    fun delete(id: MemoryId)

    fun restore(id: MemoryId)
}
