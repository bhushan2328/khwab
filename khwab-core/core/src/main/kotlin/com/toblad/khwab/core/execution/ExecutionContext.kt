package com.toblad.khwab.core.execution

import com.toblad.khwab.core.model.Plan

/**
 * Holds the current state of an executing plan.
 */
data class ExecutionContext(

    val plan: Plan,

    var currentStep: Int = 0,

    var completedSteps: Int = 0,

    var failedSteps: Int = 0,

    var status: ExecutionStatus = ExecutionStatus.SUCCESS

) {

    fun totalSteps(): Int = plan.steps.size

    fun remainingSteps(): Int =
        totalSteps() - completedSteps - failedSteps

}
