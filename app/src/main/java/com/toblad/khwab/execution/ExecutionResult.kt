package com.toblad.khwab.execution

/**
 * Result of executing an Android command.
 */
data class ExecutionResult(

    /**
     * True if execution succeeded.
     */
    val success: Boolean,

    /**
     * Optional message.
     */
    val message: String? = null
)