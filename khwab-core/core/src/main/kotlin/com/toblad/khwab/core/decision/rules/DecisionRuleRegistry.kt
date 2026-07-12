package com.toblad.khwab.core.decision.rules

class DecisionRuleRegistry {

    val rules: List<DecisionRule> = listOf(
        ClarificationRule(),
        ExecuteRule()
    )
}
