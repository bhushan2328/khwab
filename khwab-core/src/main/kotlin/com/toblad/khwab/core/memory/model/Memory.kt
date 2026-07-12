package com.toblad.khwab.core.memory.model

data class Memory(
    val id: MemoryId,
    val subject: String,
    val value: String,
    val type: MemoryType,
    val metadata: MemoryMetadata,
    val versions: List<MemoryVersion> = emptyList(),
    val auditTrail: List<MemoryAudit> = emptyList()
)
