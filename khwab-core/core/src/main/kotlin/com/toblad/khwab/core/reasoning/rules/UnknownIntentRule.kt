package com.toblad.khwab.core.reasoning.rules

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import com.toblad.khwab.core.reasoning.ReasoningReport
import com.toblad.khwab.core.reasoning.ReasoningResult
import com.toblad.khwab.core.reasoning.ReasoningStatus

class UnknownIntentRule : ReasoningRule {

    override fun evaluate(intent: IntentData): ReasoningResult? {

        return if (intent.intent == IntentType.UNKNOWN) {
            ReasoningResult(
                status = ReasoningStatus.UNSUPPORTED,
                confidence = 0.0f,
                report = ReasoningReport(
                    summary = "Unknown intent.",
                    details = listOf(
                        "Parser could not determine the user's intent."
                    )
                )
            )
        } else {
            null
        }
    }
}
