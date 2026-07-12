package com.toblad.khwab.core.decision.rules

import com.toblad.khwab.core.decision.DecisionResult
import com.toblad.khwab.core.reasoning.ReasoningResult

interface DecisionRule {

    /**
     * Returns a DecisionResult if this rule applies.
     * Returns null to allow the next rule to evaluate.
     */
    fun evaluate(result: ReasoningResult): DecisionResult?
}
