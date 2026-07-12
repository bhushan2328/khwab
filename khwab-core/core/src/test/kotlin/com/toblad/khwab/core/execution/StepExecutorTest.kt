package com.toblad.khwab.core.execution

import com.toblad.khwab.core.model.ActionType
import com.toblad.khwab.core.model.PlanStep
import kotlin.test.Test
import kotlin.test.assertEquals

private class StepExecutorImpl : StepExecutor {

    override fun execute(step: PlanStep): ExecutionResult {

        return ExecutionResult(
            status = ExecutionStatus.SUCCESS,
            message = "Executed ${step.action}"
        )
    }
}

class StepExecutorTest {

    @Test
    fun executesSingleStep() {

        val executor = StepExecutorImpl()

        val step = PlanStep(
            action = ActionType.OPEN_APP,
            target = "WhatsApp"
        )

        val result = executor.execute(step)

        assertEquals(
            ExecutionStatus.SUCCESS,
            result.status
        )

        assertEquals(
            "Executed OPEN_APP",
            result.message
        )
    }
}
