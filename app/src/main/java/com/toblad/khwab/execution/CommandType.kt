package com.toblad.khwab.execution

/**
 * Supported Android command types.
 */
enum class CommandType {

    /**
     * Launch an application.
     */
    OPEN_APP,

    /**
     * Launch a specific activity.
     */
    LAUNCH_ACTIVITY,

    /**
     * Execute an accessibility action.
     */
    ACCESSIBILITY,

    /**
     * Show overlay UI.
     */
    SHOW_OVERLAY,

    /**
     * Hide overlay UI.
     */
    HIDE_OVERLAY,

    /**
     * Speak text using TTS.
     */
    SPEAK,

    /**
     * Show a notification.
     */
    SHOW_NOTIFICATION
}