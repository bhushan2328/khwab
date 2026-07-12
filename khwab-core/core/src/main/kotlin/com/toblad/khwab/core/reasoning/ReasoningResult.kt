package com.toblad.khwab.core.reasoning

data class ReasoningResult(
    val status: ReasoningStatus,
    val confidence: Float,
    val report: ReasoningReport
)
