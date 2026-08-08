package com.toblad.khwab.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.toblad.khwab.logging.LogModule
import com.toblad.khwab.logging.Logger
import java.util.concurrent.atomic.AtomicReference

/**
 * Khwab Accessibility Service.
 *
 * Responsibilities (Phase 1):
 *   1. Maintain a live singleton reference so VoiceService / Executor can reach it.
 *   2. Track the currently-active package and window title.
 *   3. Expose [captureScreenText] to read all visible text from the current window.
 *   4. Expose low-level action helpers that AccessibilityExecutor will call in Phase 4:
 *      [performClick], [performLongClick], [performScroll], [performTypeText],
 *      [performGlobalBack], [performGlobalHome].
 *
 * Note: The user must enable this service manually in:
 *   Settings → Accessibility → Downloaded Apps → Khwab
 */
class KhwabAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KhwabA11y"

        /**
         * Live singleton reference.
         * Null when the service is not enabled or has been destroyed by the OS.
         * Always null-check before use.
         */
        val instance: AtomicReference<KhwabAccessibilityService?> =
            AtomicReference(null)
    }

    // ── State ─────────────────────────────────────────────────────────────────

    @Volatile
    var activePackage: String? = null
        private set

    @Volatile
    var activeWindowTitle: String? = null
        private set

    /**
     * Populated by [AccessibilityExecutor] after a READ_SCREEN action.
     * [VoiceService] reads this value to speak the result via TTS.
     * Reset to null after it is consumed.
     */
    @Volatile
    var lastScreenReadResult: String? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance.set(this)

        // Confirm the service info applied from XML; log for diagnostics.
        val info: AccessibilityServiceInfo = serviceInfo ?: AccessibilityServiceInfo()
        Log.d(TAG, "Service connected. eventTypes=${info.eventTypes} flags=${info.flags}")
        Logger.info(LogModule.ACCESSIBILITY, "KhwabAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val pkg = event.packageName?.toString()
                val title = event.text?.joinToString(" ")?.takeIf { it.isNotBlank() }
                if (pkg != null && pkg != activePackage) {
                    Log.d(TAG, "Active package: $pkg")
                    Logger.info(LogModule.ACCESSIBILITY, "Active package: $pkg")
                }
                if (pkg != null) activePackage = pkg
                if (title != null) activeWindowTitle = title
            }
            else -> { /* not needed in Phase 1 */ }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
        Logger.info(LogModule.ACCESSIBILITY, "KhwabAccessibilityService interrupted")
    }

    override fun onDestroy() {
        instance.set(null)
        Logger.info(LogModule.ACCESSIBILITY, "KhwabAccessibilityService destroyed")
        super.onDestroy()
    }

    // ── Screen Reading ────────────────────────────────────────────────────────

    /**
     * Walks the accessibility node tree of the currently-active window and
     * collects all non-blank visible text + content descriptions.
     *
     * Returns a newline-separated string suitable for display or TTS.
     * Returns null if the service has no accessible window root.
     *
     * Note: [AccessibilityNodeInfo.recycle] is deprecated on API 33+ (nodes are
     * managed automatically), but is still required on older API levels.
     * The @Suppress annotation silences the warning across both paths.
     */
    @Suppress("DEPRECATION")
    fun captureScreenText(): String? {
        val root = rootInActiveWindow ?: run {
            Log.w(TAG, "captureScreenText: no root window available")
            return null
        }

        val lines = mutableListOf<String>()
        collectText(root, lines)
        root.recycle()

        return lines.joinToString("\n").takeIf { it.isNotBlank() }
    }

    @Suppress("DEPRECATION")
    private fun collectText(node: AccessibilityNodeInfo, out: MutableList<String>) {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrBlank()) out.add(text)
        else if (!desc.isNullOrBlank()) out.add(desc)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, out)
            child.recycle()
        }
    }

    // ── Action Helpers ────────────────────────────────────────────────────────
    // These will be called by AccessibilityExecutor (Phase 4).
    // Placing them here in Phase 1 so the full skeleton is in place.

    /**
     * Clicks the first node whose visible text or content description matches
     * [target] (case-insensitive, substring).
     *
     * Returns true if a matching clickable node was found and clicked.
     */
    @Suppress("DEPRECATION")
    fun performClick(target: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            findAndAct(root, target) { node ->
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        } finally {
            root.recycle()
        }
    }

    /**
     * Long-clicks the first node whose text/contentDescription matches [target].
     */
    @Suppress("DEPRECATION")
    fun performLongClick(target: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            findAndAct(root, target) { node ->
                node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            }
        } finally {
            root.recycle()
        }
    }

    /**
     * Types [text] into the first editable node matching [target].
     * If [target] is blank, uses the currently-focused node.
     */
    @Suppress("DEPRECATION")
    fun performTypeText(target: String, text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            val node = if (target.isBlank()) {
                findFocused(root)
            } else {
                findNode(root, target)
            } ?: return false

            // Focus the node first so the keyboard (if any) does not interfere.
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } finally {
            root.recycle()
        }
    }

    /**
     * Scrolls the first scrollable node in the tree.
     *
     * [direction] must be "down", "up", "forward", or "backward"
     * (case-insensitive). Defaults to scroll-forward.
     */
    @Suppress("DEPRECATION")
    fun performScroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            val action = when (direction.trim().lowercase()) {
                "up", "backward" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                else             -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }
            val scrollable = findScrollable(root) ?: return false
            scrollable.performAction(action)
        } finally {
            root.recycle()
        }
    }

    /** Navigates back — equivalent to the system Back button. */
    fun performGlobalBack(): Boolean =
        performGlobalAction(GLOBAL_ACTION_BACK)

    /** Returns to the Home screen. */
    fun performGlobalHome(): Boolean =
        performGlobalAction(GLOBAL_ACTION_HOME)

    // ── Private node-search helpers ───────────────────────────────────────────

    /**
     * Finds the first node whose text or contentDescription contains [target]
     * (case-insensitive) and executes [action] on it.
     */
    private fun findAndAct(
        root: AccessibilityNodeInfo,
        target: String,
        action: (AccessibilityNodeInfo) -> Boolean
    ): Boolean {
        val node = findNode(root, target) ?: return false
        return action(node)
    }

    /**
     * Returns the first node whose text or contentDescription contains [query]
     * (case-insensitive). Does NOT recycle the returned node.
     */
    private fun findNode(root: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val q = query.lowercase()

        // Try the fast text-based search first.
        val byText = root.findAccessibilityNodeInfosByText(query)
        if (!byText.isNullOrEmpty()) return byText.first()

        // Fall back to a full-tree walk for contentDescription matches.
        return findByDescription(root, q)
    }

    @Suppress("DEPRECATION")
    private fun findByDescription(
        node: AccessibilityNodeInfo,
        query: String
    ): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase()
        if (desc != null && desc.contains(query)) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findByDescription(child, query)
            if (found != null) {
                if (found !== child) child.recycle()
                return found
            }
            child.recycle()
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollable(child)
            if (found != null) {
                if (found !== child) child.recycle()
                return found
            }
            child.recycle()
        }
        return null
    }

    private fun findFocused(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
    }
}
