package com.toblad.khwab.core.parser

import com.toblad.khwab.core.model.IntentType

object KeywordDictionary {

    val keywords = mapOf(
        IntentType.OPEN_APP to listOf("open", "launch", "start"),
        IntentType.CALL_CONTACT to listOf("call", "dial", "phone"),
        IntentType.SEND_MESSAGE to listOf("message", "sms", "text"),
        IntentType.PLAY_MEDIA to listOf("play", "music", "song"),
        IntentType.CHANGE_SETTING to listOf("wifi", "bluetooth", "setting"),
        IntentType.SEARCH_WEB to listOf("search", "google", "find"),
        IntentType.GET_TIME to listOf("time", "clock"),
        IntentType.GET_DATE to listOf("date", "day")
    )
}
