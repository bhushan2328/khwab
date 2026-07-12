package com.toblad.khwab.core.memory.permanent

import com.toblad.khwab.core.memory.model.Memory
import com.toblad.khwab.core.memory.model.MemoryAudit
import com.toblad.khwab.core.memory.model.MemoryId
import com.toblad.khwab.core.memory.model.MemoryVersion

interface PermanentMemory {

    fun create(memory: Memory): Memory

    fun read(id: MemoryId): Memory?

    fun update(memory: Memory): Memory

    fun delete(id: MemoryId): Boolean

    fun restore(id: MemoryId): Boolean

    fun history(id: MemoryId): List<MemoryVersion>

    fun audit(id: MemoryId): List<MemoryAudit>

    fun explain(id: MemoryId): String

    fun findDuplicate(subject: String): Memory?

    fun search(query: String): List<Memory>

    fun all(): List<Memory>
}
