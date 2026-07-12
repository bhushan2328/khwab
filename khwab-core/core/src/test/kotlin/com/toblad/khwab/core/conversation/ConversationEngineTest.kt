package com.toblad.khwab.core.conversation

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationEngineTest {

    @Test
    fun updatesSessionContext() {

        val engine = ConversationEngine()

        engine.process(
            IntentData(
                intent = IntentType.OPEN_APP,
                originalText = "WhatsApp"
            )
        )

        assertEquals(
            "WhatsApp",
            engine.session.context.activeApp
        )

        assertEquals(
            "WhatsApp",
            engine.session.history.latest()
        )
    }

    @Test
    fun resolvesPronoun() {

        val engine = ConversationEngine()

        engine.process(
            IntentData(
                intent = IntentType.OPEN_APP,
                originalText = "WhatsApp"
            )
        )

        val result = engine.resolve("it")

        assertTrue(result.resolved)
        assertEquals(
            "WhatsApp",
            result.resolvedReference
        )
    }
}
