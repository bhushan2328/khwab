package com.toblad.khwab.core.conversation

import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationHistoryTest {

    @Test
    fun remembersHistory() {

        val history = ConversationHistory()

        history.add("Open WhatsApp")
        history.add("Open Spotify")
        history.add("Play Music")

        assertEquals(
            "Play Music",
            history.latest()
        )

        assertEquals(
            "Open Spotify",
            history.previous()
        )

        assertEquals(
            3,
            history.all().size
        )
    }
}
