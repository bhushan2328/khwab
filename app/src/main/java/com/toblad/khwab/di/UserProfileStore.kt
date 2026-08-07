package com.toblad.khwab.di

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight plain [SharedPreferences] store for user-facing display preferences.
 *
 * Intentionally NOT encrypted — this data is not sensitive.
 * For sensitive credentials use [com.toblad.khwab.security.ApiKeyStore].
 */
object UserProfileStore {

    private const val PREFS_NAME = "khwab_user_profile"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val DEFAULT_NAME = "there"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDisplayName(context: Context): String =
        prefs(context).getString(KEY_DISPLAY_NAME, DEFAULT_NAME)
            ?.takeIf { it.isNotBlank() } ?: DEFAULT_NAME

    fun setDisplayName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_DISPLAY_NAME, name.trim()).apply()
    }
}
