package com.toblad.khwab.core.execution

import com.toblad.khwab.core.model.ActionType
import com.toblad.khwab.core.model.Plan
import com.toblad.khwab.core.model.PlanStep
import kotlin.test.Test
import kotlin.test.assertEquals

private class SuccessExecutor : StepExecutor {
    override fun execute(step: PlanStep): ExecutionResult =
        ExecutionResult(
            status = ExecutionStatus.SUCCESS,
            message = "OK"
        )
}

private class FailureExecutor : StepExecutor {
    override fun execute(step: PlanStep): ExecutionResult =
        ExecutionResult(
            status = ExecutionStatus.FAILED,
            message = "FAILED"
        )
}

private class MixedExecutor : StepExecutor {

    private var count = 0

    override fun execute(step: PlanStep): ExecutionResult {

        count++

        return if (count % 2 == 1) {
            ExecutionResult(
                status = ExecutionStatus.SUCCESS,
                message = "OK"
            )
        } else {
            ExecutionResult(
                status = ExecutionStatus.FAILED,
                message = "FAILED"
            )
        }
    }
}

class ExecutionStatusCoverageTest {

    @Test
    fun emptyPlanReturnsSuccess() {

        val context = ExecutionContext(
            Plan(emptyList())
        )

        val result = ExecutionEngine(
            SuccessExecutor()
        ).execute(context)

        assertEquals(
            ExecutionStatus.SUCCESS,
            result.status
        )

        assertEquals(0, context.completedSteps)
        assertEquals(0, context.failedSteps)
    }

    @Test
    fun failedExecutionReturnsFailed() {

        val plan = Plan(
            listOf(
                PlanStep(ActionType.OPEN_APP, "WhatsApp")
            )
        )

        val context = ExecutionContext(plan)

        val result = ExecutionEngine(
            FailureExecutor()
        ).execute(context)

        assertEquals(
            ExecutionStatus.FAILED,
            result.status
        )

        assertEquals(0, context.completedSteps)
        assertEquals(1, context.failedSteps)
    }

    @Test
    fun mixedExecutionReturnsPartialSuccess() {

        val plan = Plan(
            listOf(
                PlanStep(ActionType.OPEN_APP, "WhatsApp"),
                PlanStep(ActionType.SEARCH_WEB, "Khwab")
            )
        )

        val context = ExecutionContext(plan)

        val result = ExecutionEngine(
            MixedExecutor()
        ).execute(context)

        assertEquals(
            ExecutionStatus.PARTIAL_SUCCESS,
            result.status
        )

        assertEquals(1, context.completedSteps)
        assertEquals(1, context.failedSteps)
    }
}
