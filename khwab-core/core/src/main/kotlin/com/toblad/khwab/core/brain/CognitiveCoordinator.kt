package com.toblad.khwab.core.brain

import com.toblad.khwab.core.planner.GoalPlanner
import com.toblad.khwab.core.understanding.UnderstandingEngine

/**
 * Coordinates the complete cognitive pipeline.
 *
 * Epoch II entry point.
 */
class CognitiveCoordinator(
    private val understandingEngine: UnderstandingEngine = UnderstandingEngine(),
    private val goalPlanner: GoalPlanner = GoalPlanner(),
    private val brain: BrainV2 = BrainV2()
) {

    fun process(text: String): CognitiveResult {

        val understanding = understandingEngine.understand(text)

        goalPlanner.createGoal(understanding)

        val firstIntent = understanding.compoundIntent.intents.firstOrNull()
            ?: error("No intent found.")

        return brain.process(firstIntent)
    }
}
