package com.toblad.khwab.service

import android.view.accessibility.AccessibilityNodeInfo
import com.toblad.khwab.integration.model.screen.ScreenElement
import com.toblad.khwab.integration.model.screen.ScreenSnapshot

/**
 * Converts an Android AccessibilityNodeInfo tree into a [ScreenSnapshot].
 *
 * This is the ONLY place in the entire codebase that touches AccessibilityNodeInfo.
 * Everything above this layer (khwab-integration, khwab-core) sees only [ScreenSnapshot]
 * and [ScreenElement], which are platform-independent.
 */
object AccessibilityTreeMapper {

    /**
     * Captures the current screen from [KhwabAccessibilityService] and returns
     * a [ScreenSnapshot], or null if the service is unavailable.
     */
    @Suppress("DEPRECATION")
    fun capture(): ScreenSnapshot? {
        val service = KhwabAccessibilityService.instance.get() ?: return null
        val root = service.rootInActiveWindow ?: return null

        return try {
            val counter = Counter()
            val rootElements = listOf(mapNode(root, counter))
            ScreenSnapshot(
                packageName = service.activePackage ?: "unknown",
                windowTitle = service.activeWindowTitle,
                rootElements = rootElements
            )
        } finally {
            root.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private fun mapNode(node: AccessibilityNodeInfo, counter: Counter): ScreenElement {
        val id = "node_${counter.next()}"

        val children = mutableListOf<ScreenElement>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            children.add(mapNode(child, counter))
            child.recycle()
        }

        return ScreenElement(
            nodeId            = id,
            text              = node.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
            contentDescription = node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() },
            className         = node.className?.toString(),
            isClickable       = node.isClickable,
            isScrollable      = node.isScrollable,
            isEditable        = node.isEditable,
            isFocused         = node.isFocused,
            isVisible         = node.isVisibleToUser,
            children          = children
        )
    }

    /** Simple thread-unsafe counter used within a single capture() call. */
    private class Counter {
        private var value = 0
        fun next(): Int = value++
    }
}
