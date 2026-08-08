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
 *   CLICK            — tap the first node matching [ExecutionPlan.target]
 *   LONG_CLICK       — long-press the first matching node
 *   SCROLL           — scroll the first scrollable node; direction in parameters["direction"]
 *   SCROLL_TO_TOP    — scroll to the very top of the first scrollable container
 *   SCROLL_TO_BOTTOM — scroll to the very bottom of the first scrollable container
 *   SWIPE            — dispatch a horizontal swipe gesture; direction in parameters["direction"]
 *   TYPE_TEXT        — set text on the first editable node matching target (or focused node)
 *   GO_BACK          — global system Back action
 *   GO_HOME          — global system Home action
 *   READ_SCREEN      — collect screen text and store for TTS
 *   FIND_ELEMENT     — locate a node without acting on it (returns true if found)
 *   FOCUS_ELEMENT    — give input focus to the first matching node
 *
 * Error recovery: if CLICK or LONG_CLICK fails on the first try, the executor
 * re-reads the current screen root and retries once with the system
 * findAccessibilityNodeInfosByText fast-path.
 */
class AccessibilityExecutor : AndroidExecutor {

    companion object {
        private const val TAG = "AccessibilityExecutor"

        private val SUPPORTED_ACTIONS = setOf(
            "CLICK", "LONG_CLICK", "SCROLL",
            "SCROLL_TO_TOP", "SCROLL_TO_BOTTOM", "SWIPE",
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
                // First attempt via KhwabAccessibilityService (full tree walk + description match).
                val first = service.performClick(target)
                if (first) return true
                // Error recovery: re-read screen root and retry once with text-only fast path.
                Logger.info(LogModule.ACCESSIBILITY, "CLICK retry target='$target'")
                retryClickByText(service, target)
            }

            "LONG_CLICK" -> {
                Logger.info(LogModule.ACCESSIBILITY, "LONG_CLICK target='$target'")
                val first = service.performLongClick(target)
                if (first) return true
                Logger.info(LogModule.ACCESSIBILITY, "LONG_CLICK retry target='$target'")
                retryLongClickByText(service, target)
            }

            "SCROLL" -> {
                val direction = plan.parameters["direction"] ?: "down"
                Logger.info(LogModule.ACCESSIBILITY, "SCROLL direction=$direction")
                service.performScroll(direction)
            }

            "SCROLL_TO_TOP" -> {
                Logger.info(LogModule.ACCESSIBILITY, "SCROLL_TO_TOP")
                service.performScrollToTop()
            }

            "SCROLL_TO_BOTTOM" -> {
                Logger.info(LogModule.ACCESSIBILITY, "SCROLL_TO_BOTTOM")
                service.performScrollToBottom()
            }

            "SWIPE" -> {
                val direction = plan.parameters["direction"] ?: "left"
                Logger.info(LogModule.ACCESSIBILITY, "SWIPE direction=$direction")
                service.performSwipe(direction)
            }

            "TYPE_TEXT" -> {
                // The text to type is in target; if there's a separate "element" field
                // in parameters we use that as the field target.
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

    // ── Error recovery helpers ────────────────────────────────────────────────

    /**
     * Retry a click using the system [AccessibilityNodeInfo.findAccessibilityNodeInfosByText]
     * fast-path on a freshly captured root.  This covers cases where the tree was stale
     * on the first attempt (e.g. a screen transition just completed).
     */
    @Suppress("DEPRECATION")
    private fun retryClickByText(
        service: KhwabAccessibilityService,
        target: String
    ): Boolean {
        val root = service.rootInActiveWindow ?: return false
        return try {
            val nodes = root.findAccessibilityNodeInfosByText(target)
            if (nodes.isNullOrEmpty()) {
                Logger.info(LogModule.ACCESSIBILITY, "CLICK retry: node not found for '$target'")
                false
            } else {
                Logger.info(LogModule.ACCESSIBILITY, "CLICK retry: found node, clicking")
                nodes.first().performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
            }
        } finally {
            root.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private fun retryLongClickByText(
        service: KhwabAccessibilityService,
        target: String
    ): Boolean {
        val root = service.rootInActiveWindow ?: return false
        return try {
            val nodes = root.findAccessibilityNodeInfosByText(target)
            if (nodes.isNullOrEmpty()) false
            else nodes.first().performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK)
        } finally {
            root.recycle()
        }
    }
}
