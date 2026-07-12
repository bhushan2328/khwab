package com.toblad.khwab.core.brain

import com.toblad.khwab.core.decision.DecisionResult
import com.toblad.khwab.core.execution.ExecutionResult
import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.Plan
import com.toblad.khwab.core.reasoning.ReasoningResult

data class CognitiveResult(

    val intent: IntentData,

    val reasoning: ReasoningResult,

    val decision: DecisionResult,

    val plan: Plan? = null,

    val execution: ExecutionResult? = null
)
