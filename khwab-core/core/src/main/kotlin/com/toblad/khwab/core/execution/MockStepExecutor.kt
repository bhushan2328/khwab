package com.toblad.khwab.core.execution

import com.toblad.khwab.core.model.PlanStep

/**
 * Mock implementation used for testing.
 *
 * Always returns SUCCESS.
 */
class MockStepExecutor : StepExecutor {

    override fun execute(
        step: PlanStep
    ): ExecutionResult {

        return ExecutionResult(
            status = ExecutionStatus.SUCCESS,
            message = "Mock executed ${step.action}"
        )

    }

}
