package com.toblad.khwab.core.planner

/**
 * Represents the user's high-level goal.
 */
data class Goal(
    val type: GoalType,
    val description: String
)
