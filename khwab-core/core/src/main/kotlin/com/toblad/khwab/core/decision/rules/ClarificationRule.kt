package com.toblad.khwab.core.decision.rules

import com.toblad.khwab.core.decision.Decision
import com.toblad.khwab.core.decision.DecisionResult
import com.toblad.khwab.core.reasoning.ReasoningResult
import com.toblad.khwab.core.reasoning.ReasoningStatus

class ClarificationRule : DecisionRule {

    override fun evaluate(result: ReasoningResult): DecisionResult? {

        return if (result.status == ReasoningStatus.NEEDS_CLARIFICATION) {
            DecisionResult(
                decision = Decision.ASK_USER,
                reason = "More information is required."
            )
        } else {
            null
        }
    }
}
