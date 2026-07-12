package com.toblad.khwab.core.execution

/**
 * Result returned by an Executor after processing a Plan.
 */
data class ExecutionResult(

    val status: ExecutionStatus,

    val message: String = ""

)
