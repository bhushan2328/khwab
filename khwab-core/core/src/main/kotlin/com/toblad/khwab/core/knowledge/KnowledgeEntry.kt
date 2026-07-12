package com.toblad.khwab.core.knowledge

/**
 * Represents one piece of knowledge.
 */
data class KnowledgeEntry(

    val key: String,

    val value: String,

    val source: String = "user"

)
