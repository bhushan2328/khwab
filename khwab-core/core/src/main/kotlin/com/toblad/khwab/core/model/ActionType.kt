package com.toblad.khwab.core.model

/**
 * Represents executable actions produced by the Brain.
 *
 * These actions are platform-independent and will later be
 * translated into Android operations by the executor.
 */
enum class ActionType {

    OPEN_APP,

    CALL_CONTACT,

    SEND_MESSAGE,

    PLAY_MEDIA,

    CHANGE_SETTING,

    SEARCH_WEB,

    GET_TIME,

    GET_DATE,

    REMEMBER,

    RECALL,

    FORGET,

    UNKNOWN
}
