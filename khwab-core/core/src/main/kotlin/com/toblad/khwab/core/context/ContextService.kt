package com.toblad.khwab.core.context

/**
 * Public API for conversation context.
 */
interface ContextService {

    fun resolve(reference: String): ContextResult

    fun setActiveApp(app: String)

    fun setActiveContact(contact: String)

    fun currentContext(): ConversationContext
}
