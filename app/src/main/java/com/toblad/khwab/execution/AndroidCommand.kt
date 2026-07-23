package com.toblad.khwab.execution

data class AndroidCommand(
    val type: CommandType,
    val target: String? = null,
    val parameters: Map<String, String> = emptyMap()
)