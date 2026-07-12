package com.toblad.khwab.core.context

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContextResolverTest {

    @Test
    fun resolvesContactReference() {

        val context = ConversationContext(
            activeContact = "John"
        )

        val result = ContextResolver().resolve(
            "him",
            context
        )

        assertTrue(result.resolved)
        assertEquals("John", result.resolvedReference)
    }

    @Test
    fun resolvesAppReference() {

        val context = ConversationContext(
            activeApp = "WhatsApp"
        )

        val result = ContextResolver().resolve(
            "it",
            context
        )

        assertTrue(result.resolved)
        assertEquals("WhatsApp", result.resolvedReference)
    }

    @Test
    fun unresolvedReferenceReturnsFalse() {

        val context = ConversationContext()

        val result = ContextResolver().resolve(
            "him",
            context
        )

        assertFalse(result.resolved)
    }
}
