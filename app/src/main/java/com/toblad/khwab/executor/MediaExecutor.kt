package com.toblad.khwab.executor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.toblad.khwab.integration.model.execution.ExecutionPlan

/**
 * Handles PLAY_MEDIA actions.
 *
 * [ExecutionPlan.target] — song, artist, playlist, or media title.
 *
 * Strategy:
 *   1. Try to open Spotify with a search query.
 *   2. Fall back to YouTube search in the browser.
 */
class MediaExecutor(private val context: Context) : AndroidExecutor {

    override fun supports(action: String): Boolean =
        action.equals("PLAY_MEDIA", ignoreCase = true)

    override fun execute(plan: ExecutionPlan): Boolean {
        val query = plan.target?.trim() ?: return false

        // Try Spotify deep-link first
        if (trySpotify(query)) return true

        // Fall back to YouTube browser search
        return tryYouTube(query)
    }

    private fun trySpotify(query: String): Boolean {
        return try {
            val encoded = Uri.encode(query)
            val uri = Uri.parse("spotify:search:$encoded")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Only launch if Spotify is installed
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
                Log.d("MediaExecutor", "Opened Spotify for: $query")
                true
            } else false
        } catch (e: Exception) { false }
    }

    private fun tryYouTube(query: String): Boolean {
        return try {
            val encoded = Uri.encode(query)
            val uri = Uri.parse("https://www.youtube.com/results?search_query=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d("MediaExecutor", "Opened YouTube for: $query")
            true
        } catch (e: Exception) {
            Log.e("MediaExecutor", "Failed to open media: ${e.message}")
            false
        }
    }
}
