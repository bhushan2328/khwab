package com.toblad.khwab.core.reasoning

data class ReasoningReport(
    val summary: String,
    val details: List<String> = emptyList()
)
