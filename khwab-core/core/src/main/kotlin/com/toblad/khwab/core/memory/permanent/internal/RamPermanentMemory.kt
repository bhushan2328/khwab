package com.toblad.khwab.core.memory.permanent.internal

import com.toblad.khwab.core.memory.model.Memory
import com.toblad.khwab.core.memory.model.MemoryAudit
import com.toblad.khwab.core.memory.model.MemoryId
import com.toblad.khwab.core.memory.model.MemoryVersion
import com.toblad.khwab.core.memory.permanent.PermanentMemory

class RamPermanentMemory : PermanentMemory {

    private val memories = mutableMapOf<MemoryId, Memory>()

    override fun create(memory: Memory): Memory {
        memories[memory.id] = memory
        return memory
    }

    override fun read(id: MemoryId): Memory? =
        memories[id]

    override fun update(memory: Memory): Memory {
        memories[memory.id] = memory
        return memory
    }

    override fun delete(id: MemoryId): Boolean =
        memories.remove(id) != null

    override fun restore(id: MemoryId): Boolean =
        false

    override fun history(id: MemoryId): List<MemoryVersion> =
        emptyList()

    override fun audit(id: MemoryId): List<MemoryAudit> =
        emptyList()

    override fun explain(id: MemoryId): String =
        "Not implemented"

    override fun findDuplicate(subject: String): Memory? =
        memories.values.firstOrNull { it.subject == subject }

    override fun search(query: String): List<Memory> =
        memories.values.filter {
            it.subject.contains(query, true) ||
            it.value.contains(query, true)
        }

    override fun all(): List<Memory> =
        memories.values.toList()
}
