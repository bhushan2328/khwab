package com.toblad.khwab.core.model

data class Command(
    val text: String,
    val intent: IntentData,
    val timestamp: Long = System.currentTimeMillis()
)
