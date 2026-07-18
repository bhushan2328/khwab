package com.toblad.khwab.executor

import android.content.Context
import android.content.Intent
import android.util.Log
import com.toblad.khwab.integration.model.execution.ExecutionPlan

class AppExecutor(
    private val context: Context
) : AndroidExecutor {

    override fun supports(action: String): Boolean {
        return action.equals("OPEN_APP", ignoreCase = true)
    }

    override fun execute(plan: ExecutionPlan): Boolean {

        val packageName = plan.target ?: return false

        return try {

            val launchIntent: Intent? =
                context.packageManager.getLaunchIntentForPackage(packageName)

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)

                Log.d("AppExecutor", "Opened $packageName")
                true
            } else {
                Log.e("AppExecutor", "Package not found: $packageName")
                false
            }

        } catch (e: Exception) {

            Log.e("AppExecutor", e.message ?: "Unknown error")
            false
        }
    }
}
