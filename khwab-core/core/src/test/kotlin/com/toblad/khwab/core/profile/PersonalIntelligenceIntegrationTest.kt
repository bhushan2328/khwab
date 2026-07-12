package com.toblad.khwab.core.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class PersonalIntelligenceIntegrationTest {

    @Test
    fun favoriteAppFlow() {

        val preferences = PreferenceService()
        preferences.setFavoriteApp("Spotify")

        val resolver = PersonalReferenceResolver(
            preferences = preferences
        )

        assertEquals(
            "Spotify",
            resolver.resolve("my favorite app")
        )
    }

    @Test
    fun favoriteFoodFlow() {

        val preferences = PreferenceService()
        preferences.setFavoriteFood("Paneer Tikka")

        val resolver = PersonalReferenceResolver(
            preferences = preferences
        )

        assertEquals(
            "Paneer Tikka",
            resolver.resolve("my favorite food")
        )
    }

    @Test
    fun relationshipFlow() {

        val relationships = RelationshipService()

        relationships.rememberRelationship(
            "wife",
            "Kajal"
        )

        val resolver = PersonalReferenceResolver(
            relationships = relationships
        )

        assertEquals(
            "Kajal",
            resolver.resolve("my wife")
        )
    }
}
