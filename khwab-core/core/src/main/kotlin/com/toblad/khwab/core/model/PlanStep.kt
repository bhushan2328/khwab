package com.toblad.khwab.core.model

data class PlanStep(

    val action: ActionType,

    val target: String = "",

    val parameters: Map<String, String> = emptyMap(),

    val completed: Boolean = false

)
