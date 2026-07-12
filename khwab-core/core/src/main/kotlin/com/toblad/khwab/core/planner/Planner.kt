package com.toblad.khwab.core.planner

import com.toblad.khwab.core.model.ActionType
import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.Plan
import com.toblad.khwab.core.model.PlanStep

class Planner {

    fun createPlan(intent: IntentData): Plan {

        val step = PlanStep(
            action = ActionType.valueOf(intent.intent.name),
            target = intent.entities.firstOrNull()?.value ?: ""
        )

        return Plan(
            steps = listOf(step)
        )
    }
}
