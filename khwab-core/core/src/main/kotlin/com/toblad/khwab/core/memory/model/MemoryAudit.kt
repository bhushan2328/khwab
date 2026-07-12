package com.toblad.khwab.core.memory.model

enum class MemoryOperation {
    CREATE,
    UPDATE,
    DELETE,
    RESTORE
}

data class MemoryAudit(
    val operation: MemoryOperation,
    val timestamp: Long,
    val source: MemorySource
)
