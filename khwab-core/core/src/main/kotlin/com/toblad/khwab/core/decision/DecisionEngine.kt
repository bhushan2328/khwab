package com.toblad.khwab.core.decision

import com.toblad.khwab.core.decision.rules.DecisionRuleRegistry
import com.toblad.khwab.core.reasoning.ReasoningResult

class DecisionEngine {

    private val registry = DecisionRuleRegistry()

    fun decide(result: ReasoningResult): DecisionResult {

        for (rule in registry.rules) {

            val decision = rule.evaluate(result)

            if (decision != null) {
                return decision
            }
        }

        return DecisionResult(
            decision = Decision.BLOCK,
            reason = "No decision rule accepted the reasoning result."
        )
    }
}
