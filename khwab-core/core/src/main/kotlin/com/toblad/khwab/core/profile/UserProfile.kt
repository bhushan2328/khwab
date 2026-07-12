package com.toblad.khwab.core.profile

/**
 * Represents persistent information about the user.
 */
data class UserProfile(

    var name: String? = null,

    var favoriteApp: String? = null,

    var favoriteFood: String? = null,

    var homeCity: String? = null,

    var preferredLanguage: String? = null

)
