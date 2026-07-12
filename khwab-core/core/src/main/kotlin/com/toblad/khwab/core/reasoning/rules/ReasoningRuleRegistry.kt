package com.toblad.khwab.core.reasoning.rules

class ReasoningRuleRegistry {

    val rules: List<ReasoningRule> = listOf(
        UnknownIntentRule(),
        EntityRule()
    )
}
