package com.toblad.khwab.core.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreferenceResolverTest {

    @Test
    fun resolvesFavoriteApp() {

        val service = PreferenceService()

        service.setFavoriteApp("Spotify")

        val resolver = PreferenceResolver(service)

        assertEquals(
            "Spotify",
            resolver.resolve("my favorite app")
        )
    }

    @Test
    fun resolvesFavoriteFood() {

        val service = PreferenceService()

        service.setFavoriteFood("Paneer Tikka")

        val resolver = PreferenceResolver(service)

        assertEquals(
            "Paneer Tikka",
            resolver.resolve("favorite food")
        )
    }

    @Test
    fun unknownPreferenceReturnsNull() {

        val resolver = PreferenceResolver()

        assertNull(
            resolver.resolve("favorite movie")
        )
    }
}
