package com.toblad.khwab.core.skill

import com.toblad.khwab.core.model.ActionType
import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import com.toblad.khwab.core.model.Plan
import com.toblad.khwab.core.model.PlanStep

class MemorySkill : Skill {

    override fun supports(intent: IntentData): Boolean {
        return when (intent.intent) {
            IntentType.REMEMBER,
            IntentType.RECALL,
            IntentType.FORGET -> true
            else -> false
        }
    }

    override fun createPlan(intent: IntentData): Plan {

        val action = when (intent.intent) {
            IntentType.REMEMBER -> ActionType.REMEMBER
            IntentType.RECALL -> ActionType.RECALL
            IntentType.FORGET -> ActionType.FORGET
            else -> ActionType.UNKNOWN
        }

        val target = intent.entities.firstOrNull()?.value ?: ""

        return Plan(
            steps = listOf(
                PlanStep(
                    action = action,
                    target = target,
                    parameters = mapOf(
                        "target" to target
                    )
                )
            )
        )
    }
}
