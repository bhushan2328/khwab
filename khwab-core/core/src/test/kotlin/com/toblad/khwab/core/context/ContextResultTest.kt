package com.toblad.khwab.core.context

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContextResultTest {

    @Test
    fun unresolvedContext() {

        val result = ContextResult(
            resolved = false
        )

        assertFalse(result.resolved)
        assertNull(result.resolvedReference)
    }

    @Test
    fun resolvedContext() {

        val result = ContextResult(
            resolved = true,
            resolvedReference = "John",
            message = "Resolved successfully"
        )

        assertTrue(result.resolved)
        assertEquals("John", result.resolvedReference)
        assertEquals("Resolved successfully", result.message)
    }
}
