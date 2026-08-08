package com.toblad.khwab.executor

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.toblad.khwab.integration.model.execution.ExecutionPlan

class AppExecutor(
    private val context: Context
) : AndroidExecutor {

    override fun supports(action: String): Boolean =
        action.equals("OPEN_APP", ignoreCase = true)

    override fun execute(plan: ExecutionPlan): Boolean {
        val rawTarget = plan.target?.trim() ?: return false

        // Resolve human-readable app name (e.g. "WhatsApp") to a real package name
        // (e.g. "com.whatsapp"). If the target already looks like a package name
        // (contains a dot) skip resolution and use it directly.
        val packageName = if (rawTarget.contains('.')) rawTarget
                          else resolvePackageName(rawTarget) ?: rawTarget

        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Log.d("AppExecutor", "Opened $packageName (resolved from '$rawTarget')")
                true
            } else {
                Log.e("AppExecutor", "Package not found: $packageName (raw='$rawTarget')")
                false
            }
        } catch (e: Exception) {
            Log.e("AppExecutor", "Failed to open app: ${e.message}")
            false
        }
    }

    /**
     * Scans installed launchable apps and returns the package name whose
     * app label best matches [name] (case-insensitive).
     *
     * Matching order:
     *   1. Exact label match           "WhatsApp"      → com.whatsapp
     *   2. Label starts with name      "Chrom"         → com.android.chrome
     *   3. Label contains name         "ube"           → com.google.android.youtube
     *   4. Name contains label         user says "google maps" → matches "Maps"
     */
    private fun resolvePackageName(name: String): String? {
        val pm = context.packageManager
        val query = name.trim().lowercase()

        val apps: List<ApplicationInfo> = try {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
        } catch (_: Exception) { return null }

        fun label(info: ApplicationInfo) =
            pm.getApplicationLabel(info).toString().lowercase()

        // 1. Exact
        apps.firstOrNull { label(it) == query }?.packageName?.let { return it }
        // 2. Starts with
        apps.firstOrNull { label(it).startsWith(query) }?.packageName?.let { return it }
        // 3. Label contains query
        apps.firstOrNull { label(it).contains(query) }?.packageName?.let { return it }
        // 4. Query contains label (e.g. "google maps" contains "maps")
        apps.firstOrNull { query.contains(label(it)) && label(it).length > 3 }
            ?.packageName?.let { return it }

        return null
    }
}
