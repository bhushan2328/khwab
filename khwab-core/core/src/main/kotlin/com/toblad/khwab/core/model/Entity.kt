package com.toblad.khwab.core.model

data class Entity(
    val type: EntityType,
    val value: String,
    val confidence: Float = 1.0f
)
