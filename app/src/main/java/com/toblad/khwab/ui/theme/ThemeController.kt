package com.toblad.khwab.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeController {

    var currentTheme by mutableStateOf(ThemeMode.DEFAULT)
        private set

    fun setTheme(theme: ThemeMode) {
        currentTheme = theme
    }

    fun toggleTheme() {
        currentTheme =
            if (currentTheme == ThemeMode.DEFAULT) {
                ThemeMode.AURA
            } else {
                ThemeMode.DEFAULT
            }
    }
}
