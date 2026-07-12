package com.toblad.khwab.core.execution

import com.toblad.khwab.core.model.ActionType
import com.toblad.khwab.core.model.PlanStep
import kotlin.test.Test
import kotlin.test.assertEquals

class StepExecutionResultTest {

    @Test
    fun storesStepExecutionResult() {

        val step = PlanStep(
            action = ActionType.OPEN_APP,
            target = "WhatsApp"
        )

        val result = StepExecutionResult(
            step = step,
            status = ExecutionStatus.SUCCESS,
            message = "Executed successfully"
        )

        assertEquals(step, result.step)
        assertEquals(ExecutionStatus.SUCCESS, result.status)
        assertEquals("Executed successfully", result.message)
    }
}
