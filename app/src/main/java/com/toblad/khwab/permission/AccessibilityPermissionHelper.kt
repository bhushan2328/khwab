package com.toblad.khwab.permission

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.toblad.khwab.service.KhwabAccessibilityService

/**
 * Checks whether [KhwabAccessibilityService] is enabled by the user and provides
 * a helper to navigate to the system Accessibility Settings screen.
 *
 * The service cannot be enabled programmatically — the user must toggle it manually.
 *
 * ## Why Settings.Secure instead of AccessibilityManager.getEnabledAccessibilityServiceList()
 *
 * `getEnabledAccessibilityServiceList()` is filtered by feedback-type mask and is
 * unreliable on many OEM ROMs (MIUI, Samsung OneUI, etc.) — it often returns an
 * empty list even when the service IS enabled. Reading the raw
 * `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` string is the ground truth
 * Android itself uses; it is always accurate regardless of OEM ROM.
 */
object AccessibilityPermissionHelper {

    /**
     * Returns true if [KhwabAccessibilityService] is currently enabled and connected.
     *
     * Checks both:
     *  1. The Settings.Secure enabled-services string contains our component name.
     *  2. The live [KhwabAccessibilityService.instance] reference is non-null,
     *     meaning onServiceConnected() has fired.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return isEnabledBySystem(context) && KhwabAccessibilityService.instance.get() != null
    }

    /**
     * Returns true if the system's `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`
     * string lists [KhwabAccessibilityService], regardless of whether the live
     * instance reference is set yet.
     *
     * Use this for UI checks (e.g., deciding whether to show the onboarding dialog)
     * when the service may not yet have called onServiceConnected.
     */
    fun isEnabledBySystem(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        // The setting is a colon-separated list of "package/ServiceClass" entries.
        // Use the short class name as well as the fully-qualified name to be safe.
        val shortId  = "${context.packageName}/${KhwabAccessibilityService::class.java.simpleName}"
        val qualifiedId = "${context.packageName}/${KhwabAccessibilityService::class.java.name}"

        return enabledServices.split(':').any { entry ->
            entry.equals(shortId, ignoreCase = true) ||
            entry.equals(qualifiedId, ignoreCase = true)
        }
    }

    /**
     * Opens the system Accessibility Settings screen so the user can enable
     * [KhwabAccessibilityService].
     */
    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
