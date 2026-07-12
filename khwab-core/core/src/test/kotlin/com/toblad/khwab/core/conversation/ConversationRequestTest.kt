package com.toblad.khwab.core.conversation

import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationRequestTest {

    @Test
    fun storesConversationRequest() {

        val request = ConversationRequest(
            text = "Open WhatsApp"
        )

        assertEquals(
            "Open WhatsApp",
            request.text
        )
    }
}
