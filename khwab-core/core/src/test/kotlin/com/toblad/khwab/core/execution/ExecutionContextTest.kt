package com.toblad.khwab.core.execution

import com.toblad.khwab.core.model.ActionType
import com.toblad.khwab.core.model.Plan
import com.toblad.khwab.core.model.PlanStep
import kotlin.test.Test
import kotlin.test.assertEquals

class ExecutionContextTest {

    @Test
    fun calculatesRemainingStepsCorrectly() {

        val plan = Plan(
            steps = listOf(
                PlanStep(ActionType.OPEN_APP, "WhatsApp"),
                PlanStep(ActionType.SEARCH_WEB, "Khwab")
            )
        )

        val context = ExecutionContext(plan)

        assertEquals(2, context.totalSteps())
        assertEquals(2, context.remainingSteps())

        context.completedSteps = 1

        assertEquals(1, context.remainingSteps())
    }
}
