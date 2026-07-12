package com.toblad.khwab.core.brain

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.Plan

data class BrainResult(
    val intent: IntentData,
    val plan: Plan,
    val supported: Boolean,
    val response: String
)
