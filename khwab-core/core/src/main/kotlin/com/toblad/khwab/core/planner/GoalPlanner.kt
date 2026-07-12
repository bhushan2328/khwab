package com.toblad.khwab.core.planner

import com.toblad.khwab.core.understanding.UnderstandingResult

/**
 * Converts understanding into high-level goals.
 */
class GoalPlanner(
    private val registry: GoalRegistry = GoalRegistry()
) {

    fun createGoal(result: UnderstandingResult): GoalResult {

        val intent = result.compoundIntent.intents.firstOrNull()?.intent

        return GoalResult(
            goal = Goal(
                type = registry.resolve(intent),
                description = result.normalizedText
            )
        )
    }
}
