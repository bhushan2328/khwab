package com.toblad.khwab.core.context

/**
 * Default implementation of ContextService.
 */
class ContextEngine(

    private val context: ConversationContext = ConversationContext(),
    private val resolver: ContextResolver = ContextResolver()

) : ContextService {

    override fun resolve(reference: String): ContextResult {
        return resolver.resolve(reference, context)
    }

    override fun setActiveApp(app: String) {
        context.activeApp = app
    }

    override fun setActiveContact(contact: String) {
        context.activeContact = contact
    }

    override fun currentContext(): ConversationContext = context
}
