package com.toblad.khwab.core.knowledge

import com.toblad.khwab.core.model.IntentType

object CapabilityRegistry {

    private val supported = setOf(
        IntentType.OPEN_APP,
        IntentType.CALL_CONTACT,
        IntentType.SEND_MESSAGE,
        IntentType.PLAY_MEDIA,
        IntentType.SEARCH_WEB,
        IntentType.CHANGE_SETTING,
        IntentType.GET_TIME,
        IntentType.GET_DATE
    )

    fun supports(intent: IntentType): Boolean =
        intent in supported
}
