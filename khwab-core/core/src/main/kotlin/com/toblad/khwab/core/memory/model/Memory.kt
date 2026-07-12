package com.toblad.khwab.core.memory.model

import com.toblad.khwab.core.memory.id.MemoryIdGenerator

data class Memory(
    val id: MemoryId,
    val subject: String,
    val value: String,
    val type: MemoryType,
    val metadata: MemoryMetadata,
    val versions: List<MemoryVersion> = emptyList(),
    val auditTrail: List<MemoryAudit> = emptyList()
) {

    companion object {

        fun createPermanent(
            subject: String,
            value: String,
            category: MemoryCategory,
            source: MemorySource = MemorySource.USER,
            confidence: MemoryConfidence = MemoryConfidence.EXPLICIT
        ): Memory {

            val now = System.currentTimeMillis()

            return Memory(
                id = MemoryIdGenerator.next(),
                subject = subject,
                value = value,
                type = MemoryType.PERMANENT,
                metadata = MemoryMetadata(
                    category = category,
                    confidence = confidence,
                    source = source,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
}
