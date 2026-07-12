package com.toblad.khwab.core.context

import com.toblad.khwab.core.model.IntentData
import com.toblad.khwab.core.model.IntentType

/**
 * Updates ConversationContext from processed intents.
 */
class ContextUpdater {

    fun update(
        context: ConversationContext,
        intent: IntentData
    ) {
        when (intent.intent) {

            IntentType.OPEN_APP -> {
                context.activeApp = intent.originalText
            }

            IntentType.CALL_CONTACT,
            IntentType.SEND_MESSAGE -> {
                context.activeContact = intent.originalText
            }

            else -> {
                // Future intent handling
            }
        }
    }
}
