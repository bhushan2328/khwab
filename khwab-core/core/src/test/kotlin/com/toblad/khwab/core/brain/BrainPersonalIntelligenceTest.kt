package com.toblad.khwab.core.brain

import com.toblad.khwab.core.profile.PreferenceService
import com.toblad.khwab.core.profile.RelationshipService
import com.toblad.khwab.core.profile.PersonalReferenceResolver
import kotlin.test.Test
import kotlin.test.assertEquals

class BrainPersonalIntelligenceTest {

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
}
