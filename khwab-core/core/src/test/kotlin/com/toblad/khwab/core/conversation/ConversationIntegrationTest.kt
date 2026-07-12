package com.toblad.khwab.core.conversation

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationIntegrationTest {

    @Test
    fun multiTurnConversationWorks() {

        val engine = ConversationEngine()

        engine.process(
            IntentData(
                intent = IntentType.OPEN_APP,
                originalText = "WhatsApp"
            )
        )

        engine.process(
            IntentData(
                intent = IntentType.CALL_CONTACT,
                originalText = "John"
            )
        )

        val contact = engine.resolve("him")
        val app = engine.resolve("it")

        assertTrue(contact.resolved)
        assertEquals("John", contact.resolvedReference)

        assertTrue(app.resolved)
        assertEquals("WhatsApp", app.resolvedReference)

        assertEquals(
            "John",
            engine.session.context.activeContact
        )

        assertEquals(
            "WhatsApp",
            engine.session.context.activeApp
        )

        assertEquals(
            2,
            engine.session.history.all().size
        )
    }
}
