package com.toblad.khwab.speech

data class SpeechResult(
    val text: String,
    val isFinal: Boolean = true,
    val confidence: Float = 1.0f
)
