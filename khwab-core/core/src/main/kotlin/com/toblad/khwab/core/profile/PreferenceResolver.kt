package com.toblad.khwab.core.profile

/**
 * Resolves user preference references into concrete values.
 */
class PreferenceResolver(

    private val preferences: PreferenceService = PreferenceService()

) {

    fun resolve(reference: String): String? {

        return when (reference.trim().lowercase()) {

            "favorite app",
            "my favorite app" ->
                preferences.favoriteApp()

            "favorite food",
            "my favorite food" ->
                preferences.favoriteFood()

            else -> null
        }
    }
}
