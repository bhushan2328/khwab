package com.toblad.khwab.execution

/**
 * Base interface implemented by all Android executors.
 */
interface BaseExecutor {

    /**
     * Execute a command.
     */
    fun execute(
        command: AndroidCommand,
        context: ExecutionContext
    ): ExecutionResult
}