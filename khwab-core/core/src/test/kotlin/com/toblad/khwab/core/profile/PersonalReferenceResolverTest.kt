package com.toblad.khwab.core.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersonalReferenceResolverTest {

    @Test
    fun resolvesFavoriteApp() {

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
    fun resolvesRelationship() {

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

    @Test
    fun unknownReferenceReturnsNull() {

        val resolver = PersonalReferenceResolver()

        assertNull(
            resolver.resolve("my favorite movie")
        )
    }
}
