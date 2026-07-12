package com.toblad.khwab.core.parser

import com.toblad.khwab.core.model.IntentType

data class IntentResult(
    val intent: IntentType,
    val confidence: Float,
    val entities: Map<String, String> = emptyMap()
)
