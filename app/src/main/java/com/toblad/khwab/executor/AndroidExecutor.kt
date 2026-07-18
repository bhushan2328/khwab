package com.toblad.khwab.executor

import com.toblad.khwab.integration.model.execution.ExecutionPlan

interface AndroidExecutor {

    /**
     * Returns true if this executor can handle the given action.
     */
    fun supports(action: String): Boolean

    /**
     * Executes the given execution plan.
     *
     * Returns true if execution succeeds.
     */
    fun execute(plan: ExecutionPlan): Boolean
}
