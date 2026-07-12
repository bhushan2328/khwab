package com.toblad.khwab.core.knowledge

/**
 * Rich representation of stored knowledge.
 */
data class KnowledgeRecord(

    val key: String,

    val value: String,

    val type: KnowledgeType = KnowledgeType.UNKNOWN,

    val source: KnowledgeSource = KnowledgeSource.USER,

    val confidence: Float = 1.0f,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = createdAt

)
