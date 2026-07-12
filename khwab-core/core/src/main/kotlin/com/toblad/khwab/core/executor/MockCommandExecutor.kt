package com.toblad.khwab.core.executor

import com.toblad.khwab.core.model.Plan

class MockCommandExecutor : CommandExecutor {

    override fun execute(plan: Plan): ExecutionResult {

        return ExecutionResult(
            success = true,
            message = "Executed plan with ${plan.steps.size} step(s)."
        )
    }
}
