package com.toblad.khwab.executor

import android.util.Log
import com.toblad.khwab.integration.model.execution.ExecutionPlan
import com.toblad.khwab.logging.LogModule
import com.toblad.khwab.logging.Logger
import com.toblad.khwab.service.KhwabAccessibilityService

/**
 * Handles all screen / UI-interaction execution plans by delegating to
 * [KhwabAccessibilityService] via its live singleton reference.
 *
 * Supported actions:
 *   CLICK          — tap the first node matching [ExecutionPlan.target]
 *   LONG_CLICK     — long-press the first matching node
 *   SCROLL         — scroll the first scrollable node; direction in parameters["direction"]
 *   TYPE_TEXT      — set text on the first editable node matching target (or focused node)
 *   GO_BACK        — global system Back action
 *   GO_HOME        — global system Home action
 *   READ_SCREEN    — collect screen text and return it via ExecutionResult
 *   FIND_ELEMENT   — locate a node without acting on it (returns true if found)
 *   FOCUS_ELEMENT  — give input focus to the first matching node
 */
class AccessibilityExecutor : AndroidExecutor {

    companion object {
        private const val TAG = "AccessibilityExecutor"

        private val SUPPORTED_ACTIONS = setOf(
            "CLICK", "LONG_CLICK", "SCROLL",
            "TYPE_TEXT", "GO_BACK", "GO_HOME",
            "READ_SCREEN", "FIND_ELEMENT", "FOCUS_ELEMENT"
        )
    }

    override fun supports(action: String): Boolean =
        action.uppercase() in SUPPORTED_ACTIONS

    override fun execute(plan: ExecutionPlan): Boolean {
        val service = KhwabAccessibilityService.instance.get()

        if (service == null) {
            Log.w(TAG, "AccessibilityService not connected — cannot execute ${plan.action}")
            Logger.info(
                LogModule.ACCESSIBILITY,
                "Skipped ${plan.action}: KhwabAccessibilityService not connected"
            )
            return false
        }

        val target = plan.target.orEmpty()

        return when (plan.action.uppercase()) {

            "CLICK" -> {
                Logger.info(LogModule.ACCESSIBILITY, "CLICK target='$target'")
                service.performClick(target)
            }

            "LONG_CLICK" -> {
                Logger.info(LogModule.ACCESSIBILITY, "LONG_CLICK target='$target'")
                service.performLongClick(target)
            }

            "SCROLL" -> {
                val direction = plan.parameters["direction"] ?: "down"
                Logger.info(LogModule.ACCESSIBILITY, "SCROLL direction=$direction")
                service.performScroll(direction)
            }

            "TYPE_TEXT" -> {
                // The text to type is in target; if there's a separate "into" field
                // in parameters we use that as the element target.
                val element = plan.parameters["element"] ?: ""
                val text    = target
                Logger.info(LogModule.ACCESSIBILITY, "TYPE_TEXT element='$element' text='$text'")
                service.performTypeText(element, text)
            }

            "GO_BACK" -> {
                Logger.info(LogModule.ACCESSIBILITY, "GO_BACK")
                service.performGlobalBack()
            }

            "GO_HOME" -> {
                Logger.info(LogModule.ACCESSIBILITY, "GO_HOME")
                service.performGlobalHome()
            }

            "READ_SCREEN" -> {
                val text = service.captureScreenText()
                Logger.info(
                    LogModule.ACCESSIBILITY,
                    "READ_SCREEN captured ${text?.length ?: 0} chars"
                )
                // Store the captured text in the service so VoiceService can retrieve
                // it for TTS after execution.
                service.lastScreenReadResult = text
                text != null
            }

            "FIND_ELEMENT" -> {
                val root = service.rootInActiveWindow
                if (root == null) {
                    false
                } else {
                    val found = root.findAccessibilityNodeInfosByText(target)?.isNotEmpty() == true
                    @Suppress("DEPRECATION")
                    root.recycle()
                    Logger.info(LogModule.ACCESSIBILITY, "FIND_ELEMENT '$target' found=$found")
                    found
                }
            }

            "FOCUS_ELEMENT" -> {
                Logger.info(LogModule.ACCESSIBILITY, "FOCUS_ELEMENT target='$target'")
                // Reuse click to bring focus; a dedicated focus action can be added if needed.
                service.performClick(target)
            }

            else -> {
                Log.w(TAG, "Unhandled action: ${plan.action}")
                false
            }
        }
    }
}
