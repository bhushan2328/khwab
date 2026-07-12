package com.toblad.khwab.core.planner

import com.toblad.khwab.core.model.IntentType

/**
 * Maps intents to high-level goals.
 */
class GoalRegistry {

    private val mappings: Map<IntentType, GoalType> = mapOf(
        IntentType.OPEN_APP to GoalType.OPEN_APP,
        IntentType.SEND_MESSAGE to GoalType.SEND_MESSAGE,
        IntentType.CALL_CONTACT to GoalType.MAKE_CALL,
        IntentType.SEARCH_WEB to GoalType.SEARCH,
        IntentType.REMEMBER to GoalType.REMEMBER
    )

    fun resolve(intent: IntentType?): GoalType {
        return if (intent == null) {
            GoalType.UNKNOWN
        } else {
            mappings[intent] ?: GoalType.UNKNOWN
        }
    }
}
