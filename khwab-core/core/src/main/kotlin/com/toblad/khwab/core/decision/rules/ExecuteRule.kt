package com.toblad.khwab.core.decision.rules

import com.toblad.khwab.core.decision.Decision
import com.toblad.khwab.core.decision.DecisionResult
import com.toblad.khwab.core.reasoning.ReasoningResult
import com.toblad.khwab.core.reasoning.ReasoningStatus

class ExecuteRule : DecisionRule {

    override fun evaluate(result: ReasoningResult): DecisionResult? {

        return if (result.status == ReasoningStatus.READY) {
            DecisionResult(
                decision = Decision.EXECUTE,
                reason = "Reasoning completed successfully."
            )
        } else {
            null
        }
    }
}
