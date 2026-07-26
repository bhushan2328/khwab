package com.toblad.khwab.execution

import com.toblad.khwab.execution.executors.OpenAppExecutor
import com.toblad.khwab.integration.model.execution.ExecutionPlan

class ExecutorRegistry(
    private val context: android.content.Context
) {

    private val executors: Map<CommandType, BaseExecutor> = mapOf(
        CommandType.OPEN_APP to OpenAppExecutor()
    )

    fun execute(plan: ExecutionPlan): Boolean {

        val commandType = when (plan.action.uppercase()) {
            "OPEN_APP" -> CommandType.OPEN_APP
            else -> return false
        }

        val command = AndroidCommand(
            type = commandType,
            target = plan.target,
            parameters = plan.parameters
        )

        val executor = executors[command.type] ?: return false

        val result = executor.execute(
            command,
            ExecutionContext(context)
        )

        return result.success
    }

    fun getExecutor(command: AndroidCommand): BaseExecutor? {
        return executors[command.type]
    }
}