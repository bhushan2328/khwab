package com.toblad.khwab.core.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class PreferenceServiceTest {

    @Test
    fun remembersFavoriteApp() {

        val service = PreferenceService()

        service.setFavoriteApp("Spotify")

        assertEquals(
            "Spotify",
            service.favoriteApp()
        )
    }

    @Test
    fun remembersFavoriteFood() {

        val service = PreferenceService()

        service.setFavoriteFood("Paneer Tikka")

        assertEquals(
            "Paneer Tikka",
            service.favoriteFood()
        )
    }
}
