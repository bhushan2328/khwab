package com.toblad.khwab.core.capability

import com.toblad.khwab.core.model.IntentType

object IntentCapabilityRegistry {

    fun capability(intent: IntentType): IntentCapability {

        return when (intent) {

            IntentType.OPEN_APP,
            IntentType.CALL_CONTACT,
            IntentType.SEND_MESSAGE,
            IntentType.PLAY_MEDIA,
            IntentType.CHANGE_SETTING ->
                IntentCapability(
                    requiresEntity = true
                )

            IntentType.SEARCH_WEB,
            IntentType.GET_TIME,
            IntentType.GET_DATE,
            IntentType.REMEMBER,
            IntentType.RECALL,
            IntentType.FORGET,
            IntentType.UNKNOWN ->
                IntentCapability()
        }
    }
}
