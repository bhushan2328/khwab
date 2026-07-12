package com.toblad.khwab.core.context

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType
import kotlin.test.Test
import kotlin.test.assertEquals

class ContextUpdaterTest {

    @Test
    fun updatesActiveApp() {

        val context = ConversationContext()

        ContextUpdater().update(
            context,
            IntentData(
                intent = IntentType.OPEN_APP,
                originalText = "WhatsApp"
            )
        )

        assertEquals("WhatsApp", context.activeApp)
    }

    @Test
    fun updatesActiveContact() {

        val context = ConversationContext()

        ContextUpdater().update(
            context,
            IntentData(
                intent = IntentType.CALL_CONTACT,
                originalText = "John"
            )
        )

        assertEquals("John", context.activeContact)
    }
}
