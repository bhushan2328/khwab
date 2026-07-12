package com.toblad.khwab.core.knowledge

import com.toblad.khwab.core.model.IntentType

object CommandAliasRegistry {

    private val aliases = mapOf(
        IntentType.OPEN_APP to setOf("open","launch","start","run"),
        IntentType.CALL_CONTACT to setOf("call","dial","phone"),
        IntentType.SEND_MESSAGE to setOf("message","text","sms"),
        IntentType.PLAY_MEDIA to setOf("play","listen","music"),
        IntentType.SEARCH_WEB to setOf("search","google","find"),
        IntentType.CHANGE_SETTING to setOf("enable","disable","turn","switch"),
        IntentType.GET_TIME to setOf("time","clock"),
        IntentType.GET_DATE to setOf("date","day")
    )

    fun findIntent(word: String): IntentType? =
        aliases.entries.firstOrNull { word.lowercase() in it.value }?.key

    fun aliases(intent: IntentType): Set<String> =
        aliases[intent] ?: emptySet()
}
