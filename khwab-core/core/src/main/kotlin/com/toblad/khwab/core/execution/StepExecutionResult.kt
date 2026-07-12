package com.toblad.khwab.core.execution

import com.toblad.khwab.core.model.PlanStep

/**
 * Result of executing a single PlanStep.
 */
data class StepExecutionResult(

    val step: PlanStep,

    val status: ExecutionStatus,

    val message: String = ""

)
