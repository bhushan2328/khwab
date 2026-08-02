package com.toblad.khwab.executor

import android.util.Log
import com.toblad.khwab.aura.manager.AuraManager
import com.toblad.khwab.integration.model.execution.ExecutionPlan
import com.toblad.khwab.ui.theme.ThemeController

/**
 * Executes Aura related commands.
 */
class AuraExecutor : AndroidExecutor {

    private val auraManager = AuraManager()

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

                    auraManager.activate()

                    ThemeController.enableAura()

                    Log.d(
                        "AuraExecutor",
                        "Aura activated."
                    )

                    true
                }

                "DEACTIVATE_AURA" -> {

                    auraManager.deactivate()

                    ThemeController.disableAura()

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