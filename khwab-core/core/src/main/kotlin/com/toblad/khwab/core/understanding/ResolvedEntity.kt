package com.toblad.khwab.core.understanding

/**
 * Canonical representation of an entity after understanding.
 */
data class ResolvedEntity(
    val type: String,
    val value: String,
    val canonical: String = value.lowercase(),
    val confidence: Float = 1.0f
)
