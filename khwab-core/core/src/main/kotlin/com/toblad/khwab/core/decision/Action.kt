package com.toblad.khwab.core.decision

data class Action(
    val type: ActionType,
    val target: String,
    val parameters: Map<String, String> = emptyMap()
)
