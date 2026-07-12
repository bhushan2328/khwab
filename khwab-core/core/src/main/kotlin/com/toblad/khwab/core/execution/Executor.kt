package com.toblad.khwab.core.execution

import com.toblad.khwab.core.model.Plan

/**
 * Platform-independent execution contract.
 *
 * khwab-core defines this interface.
 * Android, Desktop or any future platform
 * will provide the implementation.
 */
interface Executor {

    fun execute(plan: Plan): ExecutionResult

}
