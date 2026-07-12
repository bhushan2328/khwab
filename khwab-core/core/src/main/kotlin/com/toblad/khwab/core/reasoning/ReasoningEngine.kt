package com.toblad.khwab.core.reasoning

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.reasoning.rules.ReasoningRuleRegistry

class ReasoningEngine {

    private val registry = ReasoningRuleRegistry()

    fun analyze(intent: IntentData): ReasoningResult {

        for (rule in registry.rules) {

            val result = rule.evaluate(intent)

            if (result != null) {
                return result
            }
        }

        return ReasoningResult(
            status = ReasoningStatus.READY,
            confidence = 1.0f,
            report = ReasoningReport(
                summary = "All reasoning checks passed.",
                details = listOf(
                    "No reasoning rules blocked execution."
                )
            )
        )
    }
}
