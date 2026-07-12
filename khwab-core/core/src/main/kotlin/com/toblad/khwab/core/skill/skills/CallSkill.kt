package com.toblad.khwab.core.skill.skills

import com.toblad.khwab.core.model.ActionType
import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import com.toblad.khwab.core.model.Plan
import com.toblad.khwab.core.model.PlanStep
import com.toblad.khwab.core.skill.Skill

/**
 * Skill responsible for planning phone call actions.
 */
class CallSkill : Skill {

    override fun supports(intent: IntentData): Boolean {
        return intent.intent == IntentType.CALL_CONTACT
    }

    override fun createPlan(intent: IntentData): Plan {

        val target = intent.entities.firstOrNull()?.value ?: ""

        return Plan(
            steps = listOf(
                PlanStep(
                    action = ActionType.CALL_CONTACT,
                    target = target
                )
            )
        )
    }
}
