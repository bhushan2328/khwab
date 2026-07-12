package com.toblad.khwab.core.profile

/**
 * Stores user relationships.
 */
class RelationshipService(

    private val relationships: MutableMap<String, String> = mutableMapOf()

) {

    fun rememberRelationship(
        relationship: String,
        person: String
    ) {
        relationships[relationship.lowercase()] = person
    }

    fun resolveRelationship(
        relationship: String
    ): String? {
        return relationships[relationship.lowercase()]
    }

    fun forgetRelationship(
        relationship: String
    ): Boolean {
        return relationships.remove(
            relationship.lowercase()
        ) != null
    }
}
