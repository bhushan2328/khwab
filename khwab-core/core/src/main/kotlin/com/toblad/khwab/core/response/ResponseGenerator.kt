package com.toblad.khwab.core.response

import com.toblad.khwab.core.model.IntentType

class ResponseGenerator {

    fun generate(intent: IntentType, target: String): String {

        return when (intent) {

            IntentType.OPEN_APP ->
                "Opening $target..."

            IntentType.CALL_CONTACT ->
                "Calling $target..."

            IntentType.SEND_MESSAGE ->
                "Sending message to $target..."

            IntentType.PLAY_MEDIA ->
                "Playing $target..."

            IntentType.CHANGE_SETTING ->
                "Changing settings..."

            IntentType.SEARCH_WEB ->
                "Searching for $target..."

            IntentType.GET_TIME ->
                "Fetching current time..."

            IntentType.GET_DATE ->
                "Fetching today's date..."

            IntentType.REMEMBER ->
                "I'll remember that."

            IntentType.RECALL ->
                "Let me recall that."

            IntentType.FORGET ->
                "I'll forget that."

            IntentType.UNKNOWN ->
                "Sorry, I don't understand that command yet."
        }
    }
}
