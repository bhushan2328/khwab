package com.toblad.khwab.execution

import com.toblad.khwab.execution.executors.OpenAppExecutor

/**
 * Maintains the mapping between command types and their executors.
 */
class ExecutorRegistry {

    private val executors: Map<CommandType, BaseExecutor> = mapOf(

        CommandType.OPEN_APP to OpenAppExecutor()

    )

    /**
     * Returns the executor for the specified command.
     */
    fun getExecutor(command: AndroidCommand): BaseExecutor? {

        return executors[command.type]

    }
}