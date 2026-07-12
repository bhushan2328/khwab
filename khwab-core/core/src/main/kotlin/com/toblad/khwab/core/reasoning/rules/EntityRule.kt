package com.toblad.khwab.core.reasoning.rules

import com.toblad.khwab.core.capability.IntentCapabilityRegistry
import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import com.toblad.khwab.core.reasoning.ReasoningReport
import com.toblad.khwab.core.reasoning.ReasoningResult
import com.toblad.khwab.core.reasoning.ReasoningStatus

class EntityRule : ReasoningRule {

    override fun evaluate(intent: IntentData): ReasoningResult? {

        if (intent.intent == IntentType.UNKNOWN) {
            return null
        }

        val capability = IntentCapabilityRegistry.capability(intent.intent)

        if (!capability.requiresEntity) {
            return null
        }

        if (intent.entities.isEmpty()) {
            return ReasoningResult(
                status = ReasoningStatus.NEEDS_CLARIFICATION,
                confidence = 0.6f,
                report = ReasoningReport(
                    summary = "Missing required entity.",
                    details = listOf(
                        "This intent requires an entity before execution."
                    )
                )
            )
        }

        return null
    }
}
