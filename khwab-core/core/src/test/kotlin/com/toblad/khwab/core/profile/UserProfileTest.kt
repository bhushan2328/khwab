package com.toblad.khwab.core.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class UserProfileTest {

    @Test
    fun storesPreferences() {

        val profile = UserProfile()

        profile.favoriteApp = "Spotify"
        profile.favoriteFood = "Paneer Tikka"

        assertEquals(
            "Spotify",
            profile.favoriteApp
        )

        assertEquals(
            "Paneer Tikka",
            profile.favoriteFood
        )
    }
}
