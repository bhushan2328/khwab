package com.toblad.khwab.core.context

/**
 * Resolves conversational references
 * using the current ConversationContext.
 */
class ContextResolver {

    fun resolve(
        reference: String,
        context: ConversationContext
    ): ContextResult {

        return when (reference.lowercase()) {

            "him", "her" ->
                context.activeContact?.let {
                    ContextResult(
                        resolved = true,
                        resolvedReference = it,
                        message = "Resolved contact"
                    )
                } ?: ContextResult(
                    resolved = false,
                    message = "No active contact"
                )

            "it", "that app" ->
                context.activeApp?.let {
                    ContextResult(
                        resolved = true,
                        resolvedReference = it,
                        message = "Resolved app"
                    )
                } ?: ContextResult(
                    resolved = false,
                    message = "No active app"
                )

            else ->
                ContextResult(
                    resolved = false,
                    message = "Unknown reference"
                )
        }
    }
}
