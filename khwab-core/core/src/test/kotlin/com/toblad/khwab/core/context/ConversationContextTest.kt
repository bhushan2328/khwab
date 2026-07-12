package com.toblad.khwab.core.context

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConversationContextTest {

    @Test
    fun defaultStateIsEmpty() {

        val context = ConversationContext()

        assertNull(context.lastIntent)
        assertNull(context.activeApp)
        assertNull(context.activeContact)
        assertNull(context.lastReference)
    }

    @Test
    fun storesConversationState() {

        val context = ConversationContext()

        context.activeApp = "WhatsApp"
        context.activeContact = "John"
        context.lastReference = "him"

        assertEquals("WhatsApp", context.activeApp)
        assertEquals("John", context.activeContact)
        assertEquals("him", context.lastReference)
    }
}
