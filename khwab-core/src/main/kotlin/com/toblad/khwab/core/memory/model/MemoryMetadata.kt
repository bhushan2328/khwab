package com.toblad.khwab.core.memory.model

data class MemoryMetadata(
    val category: MemoryCategory,
    val confidence: MemoryConfidence,
    val source: MemorySource,
    val createdAt: Long,
    val updatedAt: Long,
    val currentVersion: Int = 1,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
