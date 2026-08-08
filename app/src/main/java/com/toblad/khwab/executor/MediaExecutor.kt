package com.toblad.khwab.executor

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.util.Log
import com.toblad.khwab.integration.model.execution.ExecutionPlan

/**
 * Handles PLAY_MEDIA actions.
 *
 * [ExecutionPlan.target]             — song, artist, playlist, or media title.
 * [ExecutionPlan.parameters]["app"]  — preferred app: "spotify", "youtube",
 *                                     "youtube music", "apple music", etc.
 *
 * Resolution order:
 *   1. If parameters["app"] is set, try that specific app first.
 *   2. Try the system default music/audio app.
 *   3. Try Spotify deep-link.
 *   4. Fall back to YouTube browser search.
 */
class MediaExecutor(private val context: Context) : AndroidExecutor {

    override fun supports(action: String): Boolean =
        action.equals("PLAY_MEDIA", ignoreCase = true)

    override fun execute(plan: ExecutionPlan): Boolean {
        val query = plan.target?.trim() ?: return false
        val preferredApp = plan.parameters["app"]?.trim()?.lowercase() ?: ""

        // 1. User specified a preferred app by name.
        if (preferredApp.isNotBlank()) {
            if (tryNamedApp(preferredApp, query)) return true
        }

        // 2. System default audio/music app.
        if (trySystemDefault(query)) return true

        // 3. Spotify deep-link.
        if (trySpotify(query)) return true

        // 4. YouTube browser fallback.
        return tryYouTube(query)
    }

    /** Tries to open a specific app by name keyword. */
    private fun tryNamedApp(appName: String, query: String): Boolean {
        return when {
            "spotify" in appName              -> trySpotify(query)
            "youtube music" in appName        -> tryYouTubeMusic(query)
            "youtube" in appName              -> tryYouTube(query)
            "apple music" in appName          -> tryAppWithSearch("com.apple.android.music", query)
            "amazon" in appName               -> tryAppWithSearch("com.amazon.mp3", query)
            "jiosaavn" in appName ||
            "saavn" in appName                -> tryAppWithSearch("com.jio.media.ondemand", query)
            "gaana" in appName                -> tryAppWithSearch("com.gaana", query)
            else                              -> false
        }
    }

    /**
     * Queries the system for the default app that handles audio media.
     * Uses ACTION_VIEW with an audio MIME type to find it.
     */
    private fun trySystemDefault(query: String): Boolean {
        return try {
            val pm = context.packageManager
            val probe = Intent(Intent.ACTION_VIEW).apply {
                type = "audio/*"
            }
            val resolved: ResolveInfo? = pm.resolveActivity(probe, 0)
            val pkg = resolved?.activityInfo?.packageName ?: return false

            // Skip if it resolved to a browser or generic resolver.
            if (pkg.contains("browser") || pkg == "android") return false

            // Build a search intent for the resolved package.
            val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage(pkg)
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (pm.resolveActivity(searchIntent, 0) != null) {
                context.startActivity(searchIntent)
                Log.d("MediaExecutor", "Opened default music app ($pkg) for: $query")
                return true
            }

            // If the app doesn't handle ACTION_SEARCH, just open it.
            val launchIntent = pm.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                Log.d("MediaExecutor", "Launched default music app ($pkg)")
                return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun trySpotify(query: String): Boolean {
        return try {
            val encoded = Uri.encode(query)
            val uri = Uri.parse("spotify:search:$encoded")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
                Log.d("MediaExecutor", "Opened Spotify for: $query")
                true
            } else false
        } catch (e: Exception) { false }
    }

    private fun tryYouTubeMusic(query: String): Boolean {
        return tryAppWithSearch("com.google.android.apps.youtube.music", query)
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

    private fun tryAppWithSearch(packageName: String, query: String): Boolean {
        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage(packageName)
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (pm.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
                Log.d("MediaExecutor", "Opened $packageName for: $query")
                true
            } else {
                val launch = pm.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (launch != null) {
                    context.startActivity(launch)
                    Log.d("MediaExecutor", "Launched $packageName")
                    true
                } else false
            }
        } catch (e: Exception) { false }
    }
}
