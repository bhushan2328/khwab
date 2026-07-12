package com.toblad.khwab.core.profile

/**
 * Resolves personal references such as:
 *  - my favorite app
 *  - my favorite food
 *  - my wife
 */
class PersonalReferenceResolver(

    private val preferences: PreferenceService = PreferenceService(),
    private val relationships: RelationshipService = RelationshipService()

) {

    fun resolve(reference: String): String? {

        val text = reference.lowercase().trim()

        return when (text) {

            "my favorite app",
            "favorite app" ->
                preferences.favoriteApp()

            "my favorite food",
            "favorite food" ->
                preferences.favoriteFood()

            "my wife" ->
                relationships.resolveRelationship("wife")

            "my husband" ->
                relationships.resolveRelationship("husband")

            "my mother" ->
                relationships.resolveRelationship("mother")

            "my father" ->
                relationships.resolveRelationship("father")

            else -> null
        }
    }
}
