package com.toblad.khwab.executor

import android.content.Context
import com.toblad.khwab.integration.model.execution.ExecutionPlan

class AndroidExecutionEngine(
    private val context: Context
) {

    private val registry = ExecutorRegistry(context)

    fun execute(plan: ExecutionPlan): Boolean {
        return registry.execute(plan)
    }
}
