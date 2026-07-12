package com.toblad.khwab.core.conversation

import com.toblad.khwab.core.context.ContextResolver
import com.toblad.khwab.core.context.ContextUpdater
import com.toblad.khwab.core.model.IntentData

/**
 * Coordinates all conversation-related components.
 */
class ConversationEngine(

    private val updater: ContextUpdater = ContextUpdater(),
    private val resolver: ContextResolver = ContextResolver()

) {

    val session = ConversationSession()

    fun process(intent: IntentData) {

        session.record(intent.originalText)

        updater.update(
            session.context,
            intent
        )
    }

    fun resolve(reference: String) =
        resolver.resolve(
            reference,
            session.context
        )
}
