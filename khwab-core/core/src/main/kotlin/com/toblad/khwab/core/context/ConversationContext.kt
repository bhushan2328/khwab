package com.toblad.khwab.core.context

import com.toblad.khwab.core.model.IntentData

/**
 * Stores the active conversation state.
 *
 * This object lives only for the duration
 * of the current conversation.
 */
data class ConversationContext(

    var lastIntent: IntentData? = null,

    var activeApp: String? = null,

    var activeContact: String? = null,

    var lastReference: String? = null

)
