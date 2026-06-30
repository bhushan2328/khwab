package com.toblad.khwab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KhwabDarkColors = darkColorScheme(

    primary = KhwabBlue,
    secondary = KhwabBlueLight,

    background = KhwabBackground,
    surface = KhwabSurface,

    onPrimary = KhwabWhite,
    onSecondary = KhwabWhite,

    onBackground = KhwabWhite,
    onSurface = KhwabWhite
)

private val KhwabLightColors = lightColorScheme(

    primary = KhwabBlue,
    secondary = KhwabBlueLight,

    background = KhwabWhite,
    surface = KhwabWhite,

    onPrimary = KhwabWhite,
    onSecondary = KhwabWhite,

    onBackground = KhwabBackground,
    onSurface = KhwabBackground
)

@Composable
fun KhwabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    MaterialTheme(

        colorScheme = if (darkTheme)
            KhwabDarkColors
        else
            KhwabLightColors,

        typography = Typography,

        content = content

    )
}