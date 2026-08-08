package com.toblad.khwab.executor

import android.content.Context
import android.util.Log
import com.toblad.khwab.integration.model.execution.ExecutionPlan

class ExecutorRegistry(
    context: Context
) {

    private val executors: List<AndroidExecutor> = listOf(

        // Screen / UI interaction via KhwabAccessibilityService
        AccessibilityExecutor(),

        // Aura commands
        AuraExecutor(context),

        // App launch
        AppExecutor(context),

        // Communication
        CallExecutor(context),
        MessageExecutor(context),

        // Media playback
        MediaExecutor(context),

        // Web search
        SearchExecutor(context),

        // System settings
        SettingExecutor(context)

    )

    fun execute(
        plan: ExecutionPlan
    ): Boolean {

        val executor =
            executors.firstOrNull {
                it.supports(plan.action)
            }

        if (executor == null) {

            Log.w(
                "ExecutorRegistry",
                "No executor found for ${plan.action}"
            )

            return false
        }

        return executor.execute(plan)
    }
}