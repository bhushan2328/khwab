package com.toblad.khwab.core.execution

import com.toblad.khwab.core.model.PlanStep

/**
 * Executes a single PlanStep.
 *
 * Implementations:
 * - MockStepExecutor
 * - AndroidStepExecutor (future)
 */
interface StepExecutor {

    fun execute(
        step: PlanStep
    ): ExecutionResult

}
