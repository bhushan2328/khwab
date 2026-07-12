package com.toblad.khwab.core.model

enum class Confidence(
    val score: Float
) {
    HIGH(1.0f),
    MEDIUM(0.7f),
    LOW(0.4f),
    UNKNOWN(0.0f)
}
