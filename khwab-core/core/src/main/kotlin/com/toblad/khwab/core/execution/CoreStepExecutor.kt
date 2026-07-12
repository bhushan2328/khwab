package com.toblad.khwab.core.execution

import com.toblad.khwab.core.knowledge.KnowledgeEngine
import com.toblad.khwab.core.model.ActionType
import com.toblad.khwab.core.model.PlanStep

/**
 * Production StepExecutor backed by KnowledgeEngine.
 *
 * Currently implements memory actions.
 * Other actions will be added in future sprints.
 */
class CoreStepExecutor(
    private val knowledge: KnowledgeEngine = KnowledgeEngine()
) : StepExecutor {

    override fun execute(
        step: PlanStep
    ): ExecutionResult {

        return when (step.action) {

            ActionType.REMEMBER -> {
                knowledge.remember(
                    key = step.target,
                    value = step.parameters["value"] ?: ""
                )

                ExecutionResult(
                    status = ExecutionStatus.SUCCESS,
                    message = "Remembered '${step.target}'."
                )
            }

            ActionType.RECALL -> {

                val record = knowledge.recall(step.target)

                ExecutionResult(
                    status =
                        if (record != null)
                            ExecutionStatus.SUCCESS
                        else
                            ExecutionStatus.FAILED,

                    message =
                        record?.value
                            ?: "Nothing remembered for '${step.target}'."
                )
            }

            ActionType.FORGET -> {

                val removed = knowledge.forget(step.target)

                ExecutionResult(
                    status =
                        if (removed)
                            ExecutionStatus.SUCCESS
                        else
                            ExecutionStatus.FAILED,

                    message =
                        if (removed)
                            "Forgot '${step.target}'."
                        else
                            "Nothing to forget."
                )
            }

            else ->
                ExecutionResult(
                    status = ExecutionStatus.FAILED,
                    message = "Action ${step.action} not implemented."
                )
        }
    }
}
