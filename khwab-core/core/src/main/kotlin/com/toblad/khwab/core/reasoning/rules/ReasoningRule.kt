package com.toblad.khwab.core.reasoning.rules

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.reasoning.ReasoningResult

interface ReasoningRule {

    /**
     * Returns a ReasoningResult if this rule applies.
     * Returns null if the next rule should be evaluated.
     */
    fun evaluate(intent: IntentData): ReasoningResult?
}
