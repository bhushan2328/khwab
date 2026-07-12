package com.toblad.khwab.core.model

data class IntentData(
    val intent: IntentType = IntentType.UNKNOWN,
    val entities: List<Entity> = emptyList(),
    val originalText: String = "",
    val confidence: Float = 1.0f
)
