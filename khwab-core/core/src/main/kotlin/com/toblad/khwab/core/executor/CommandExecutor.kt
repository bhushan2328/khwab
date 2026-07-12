package com.toblad.khwab.core.executor

import com.toblad.khwab.core.model.Plan

interface CommandExecutor {

    fun execute(plan: Plan): ExecutionResult
}
