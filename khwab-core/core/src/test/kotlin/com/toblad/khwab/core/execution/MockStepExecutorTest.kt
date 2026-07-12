package com.toblad.khwab.core.execution

import com.toblad.khwab.core.model.ActionType
import com.toblad.khwab.core.model.PlanStep
import kotlin.test.Test
import kotlin.test.assertEquals

class MockStepExecutorTest {

    @Test
    fun mockExecutorReturnsSuccess() {

        val executor = MockStepExecutor()

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
            "Mock executed OPEN_APP",
            result.message
        )
    }
}
