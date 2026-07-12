package com.toblad.khwab.core.conversation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConversationSessionTest {

    @Test
    fun sessionMaintainsHistory() {

        val session = ConversationSession()

        session.record("Open WhatsApp")
        session.record("Call John")

        assertNotNull(session.id)
        assertNotNull(session.context)
        assertNotNull(session.history)

        assertEquals(
            "Call John",
            session.history.latest()
        )

        assertEquals(
            "Open WhatsApp",
            session.history.previous()
        )

        assertTrue(session.lastActivity >= session.startedAt)
    }
}
