package com.toblad.khwab.core.memory.permanent.internal

import com.toblad.khwab.core.memory.model.*
import com.toblad.khwab.core.memory.permanent.PermanentMemory

class RamPermanentMemory : PermanentMemory {

    private val memories = mutableMapOf<MemoryId, Memory>()
    private val history = mutableMapOf<MemoryId, MutableList<MemoryVersion>>()
    private val audits = mutableMapOf<MemoryId, MutableList<MemoryAudit>>()

    override fun create(memory: Memory): Memory {
        memories[memory.id] = memory

        history[memory.id] = mutableListOf(
            MemoryVersion(
                version = 1,
                value = memory.value,
                updatedAt = System.currentTimeMillis()
            )
        )

        audits[memory.id] = mutableListOf(
            MemoryAudit(
                operation = MemoryOperation.CREATE,
                timestamp = System.currentTimeMillis(),
                source = memory.metadata.source
            )
        )

        return memory
    }

    override fun read(id: MemoryId): Memory? =
        memories[id]?.takeIf { !it.metadata.isDeleted }

    override fun update(memory: Memory): Memory {

        memories[memory.id] = memory

        history.getOrPut(memory.id) { mutableListOf() }.add(
            MemoryVersion(
                version = history[memory.id]?.size?.plus(1) ?: 1,
                value = memory.value,
                updatedAt = System.currentTimeMillis()
            )
        )

        audits.getOrPut(memory.id) { mutableListOf() }.add(
            MemoryAudit(
                operation = MemoryOperation.UPDATE,
                timestamp = System.currentTimeMillis(),
                source = memory.metadata.source
            )
        )

        return memory
    }

    override fun delete(id: MemoryId): Boolean {

        val memory = memories[id] ?: return false

        memories[id] = memory.copy(
            metadata = memory.metadata.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        audits.getOrPut(id) { mutableListOf() }.add(
            MemoryAudit(
                operation = MemoryOperation.DELETE,
                timestamp = System.currentTimeMillis(),
                source = memory.metadata.source
            )
        )

        return true
    }

    override fun restore(id: MemoryId): Boolean {

        val memory = memories[id] ?: return false

        memories[id] = memory.copy(
            metadata = memory.metadata.copy(
                isDeleted = false,
                deletedAt = null,
                updatedAt = System.currentTimeMillis()
            )
        )

        audits.getOrPut(id) { mutableListOf() }.add(
            MemoryAudit(
                operation = MemoryOperation.RESTORE,
                timestamp = System.currentTimeMillis(),
                source = memory.metadata.source
            )
        )

        return true
    }

    override fun history(id: MemoryId): List<MemoryVersion> =
        history[id]?.toList() ?: emptyList()

    override fun audit(id: MemoryId): List<MemoryAudit> =
        audits[id]?.toList() ?: emptyList()

    override fun explain(id: MemoryId): String {

        val memory = memories[id]
            ?: return "Memory not found."

        val firstAudit = audits[id]?.firstOrNull()

        return if (firstAudit != null) {
            "This memory was created on ${firstAudit.timestamp} from ${firstAudit.source}."
        } else {
            "No explanation available."
        }
    }

    override fun findDuplicate(subject: String): Memory? =
        memories.values.firstOrNull {
            !it.metadata.isDeleted &&
            it.subject.equals(subject, ignoreCase = true)
        }

    override fun search(query: String): List<Memory> =
        memories.values.filter {
            !it.metadata.isDeleted &&
            (
                it.subject.contains(query, ignoreCase = true) ||
                it.value.contains(query, ignoreCase = true)
            )
        }

    override fun all(): List<Memory> =
        memories.values.filter { !it.metadata.isDeleted }
}
