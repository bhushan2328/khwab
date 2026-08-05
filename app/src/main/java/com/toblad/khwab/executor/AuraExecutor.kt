package com.toblad.khwab.executor

import android.content.Context
import android.util.Log
import com.toblad.khwab.aura.AuraBridge
import com.toblad.khwab.environment.AuraEnvironmentSync
import com.toblad.khwab.integration.model.execution.ExecutionPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Executes Aura related commands.
 *
 * Always goes through [AuraBridge] — the single shared Aura
 * instance — so that environment updates (real weather) and
 * UI theme updates stay in sync with each other.
 */
class AuraExecutor(
    context: Context
) : AndroidExecutor {

    private val environmentSync = AuraEnvironmentSync(context)

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun supports(action: String): Boolean {

        return action.equals(
            "ACTIVATE_AURA",
            ignoreCase = true
        ) ||
                action.equals(
                    "DEACTIVATE_AURA",
                    ignoreCase = true
                )
    }

    override fun execute(
        plan: ExecutionPlan
    ): Boolean {

        return try {

            when (plan.action.uppercase()) {

                "ACTIVATE_AURA" -> {

                    AuraBridge.activate()

                    // Pull in real location + live weather so
                    // the theme reflects actual conditions.
                    // Real time is already handled inside Aura.
                    scope.launch {
                        environmentSync.sync()
                    }

                    Log.d(
                        "AuraExecutor",
                        "Aura activated."
                    )

                    true
                }

                "DEACTIVATE_AURA" -> {

                    AuraBridge.deactivate()

                    Log.d(
                        "AuraExecutor",
                        "Aura deactivated."
                    )

                    true
                }

                else -> false
            }

        } catch (e: Exception) {

            Log.e(
                "AuraExecutor",
                e.message ?: "Unknown error"
            )

            false
        }
    }
}