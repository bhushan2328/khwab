package com.toblad.khwab.core.execution

class ExecutionEngine(

    private val executor: StepExecutor

) {

    fun execute(
        context: ExecutionContext
    ): ExecutionResult {

        var completed = 0
        var failed = 0

        for (step in context.plan.steps) {

            context.currentStep++

            val result = executor.execute(step)

            if (result.status == ExecutionStatus.SUCCESS) {
                completed++
            } else {
                failed++
            }
        }

        context.completedSteps = completed
        context.failedSteps = failed

        val status = when {
            failed == 0 -> ExecutionStatus.SUCCESS
            completed == 0 -> ExecutionStatus.FAILED
            else -> ExecutionStatus.PARTIAL_SUCCESS
        }

        context.status = status

        return ExecutionResult(
            status = status,
            message = "Completed=$completed Failed=$failed"
        )
    }
}
