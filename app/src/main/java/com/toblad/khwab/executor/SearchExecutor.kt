package com.toblad.khwab.executor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.toblad.khwab.integration.model.execution.ExecutionPlan

/**
 * Handles SEARCH_WEB actions.
 *
 * [ExecutionPlan.target] — the search query string.
 *
 * Opens the default browser with a Google search for the query.
 */
class SearchExecutor(private val context: Context) : AndroidExecutor {

    override fun supports(action: String): Boolean =
        action.equals("SEARCH_WEB", ignoreCase = true)

    override fun execute(plan: ExecutionPlan): Boolean {
        val query = plan.target?.trim() ?: return false
        return try {
            val encoded = Uri.encode(query)
            val uri = Uri.parse("https://www.google.com/search?q=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("SearchExecutor", "Searching for: $query")
            true
        } catch (e: Exception) {
            Log.e("SearchExecutor", "Failed to open browser: ${e.message}")
            false
        }
    }
}
