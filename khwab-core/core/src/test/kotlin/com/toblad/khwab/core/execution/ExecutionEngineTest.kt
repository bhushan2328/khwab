package com.toblad.khwab.core.execution

import com.toblad.khwab.core.model.ActionType
import com.toblad.khwab.core.model.Plan
import com.toblad.khwab.core.model.PlanStep
import kotlin.test.Test
import kotlin.test.assertEquals

private class EngineStepExecutor : StepExecutor {

    override fun execute(step: PlanStep): ExecutionResult {
        return ExecutionResult(
            status = ExecutionStatus.SUCCESS,
            message = "OK"
        )
    }
}

class ExecutionEngineTest {

    @Test
    fun executesEntirePlan() {

        val plan = Plan(
            steps = listOf(
                PlanStep(ActionType.OPEN_APP, "WhatsApp"),
                PlanStep(ActionType.SEARCH_WEB, "Khwab"),
                PlanStep(ActionType.PLAY_MEDIA, "Music")
            )
        )

        val context = ExecutionContext(plan)

        val engine = ExecutionEngine(
            EngineStepExecutor()
        )

        val result = engine.execute(context)

        assertEquals(ExecutionStatus.SUCCESS, result.status)
        assertEquals(3, context.completedSteps)
        assertEquals(0, context.failedSteps)
    }
}
