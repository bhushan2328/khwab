package com.toblad.khwab.execution.executors

import android.content.Intent
import com.toblad.khwab.execution.AndroidCommand
import com.toblad.khwab.execution.BaseExecutor
import com.toblad.khwab.execution.ExecutionContext
import com.toblad.khwab.execution.ExecutionResult

/**
 * Executes OPEN_APP commands.
 */
class OpenAppExecutor : BaseExecutor {

    override fun execute(
        command: AndroidCommand,
        context: ExecutionContext
    ): ExecutionResult {

        val packageName = command.target
            ?: return ExecutionResult(
                success = false,
                message = "Package name is missing."
            )

        val launchIntent = context.context.packageManager
            .getLaunchIntentForPackage(packageName)
            ?: return ExecutionResult(
                success = false,
                message = "Application not found."
            )

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.context.startActivity(launchIntent)

        return ExecutionResult(
            success = true,
            message = "Application launched successfully."
        )
    }
}
