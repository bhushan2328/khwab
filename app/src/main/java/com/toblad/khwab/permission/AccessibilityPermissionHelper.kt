package com.toblad.khwab.permission

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.toblad.khwab.service.KhwabAccessibilityService

/**
 * Checks whether [KhwabAccessibilityService] is enabled by the user and provides
 * a helper to navigate to the system Accessibility Settings screen.
 *
 * The service cannot be enabled programmatically — the user must toggle it manually.
 */
object AccessibilityPermissionHelper {

    /**
     * Returns true if [KhwabAccessibilityService] is currently enabled and connected.
     *
     * Two conditions must both be true:
     *  1. The service appears in the list of enabled accessibility services
     *     (confirmed by AccessibilityManager).
     *  2. The live [KhwabAccessibilityService.instance] reference is non-null,
     *     meaning onServiceConnected() has fired.
     *
     * Using both checks avoids a race window where the system reports the service as
     * enabled but it has not yet called onServiceConnected.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager
            ?: return false

        val expectedId = "${context.packageName}/${KhwabAccessibilityService::class.java.name}"

        val enabledBySystem = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any { info ->
            info.resolveInfo.serviceInfo.let { si ->
                "${si.packageName}/${si.name}" == expectedId
            }
        }

        return enabledBySystem && KhwabAccessibilityService.instance.get() != null
    }

    /**
     * Returns true if the system lists [KhwabAccessibilityService] as enabled,
     * regardless of whether the live instance reference is set.
     *
     * Use this for UI checks (e.g., deciding whether to show the onboarding dialog)
     * when the service may not yet have called onServiceConnected.
     */
    fun isEnabledBySystem(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager
            ?: return false

        val expectedId = "${context.packageName}/${KhwabAccessibilityService::class.java.name}"

        return am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any { info ->
            info.resolveInfo.serviceInfo.let { si ->
                "${si.packageName}/${si.name}" == expectedId
            }
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
