package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = HomeHelpNavy,
    onPrimary = White,
    primaryContainer = Slate100,
    onPrimaryContainer = HomeHelpNavy,
    secondary = HomeHelpGold,
    onSecondary = Slate900,
    secondaryContainer = HomeHelpGoldLight,
    onSecondaryContainer = HomeHelpGoldDark,
    tertiary = HomeHelpCyan,
    onTertiary = White,
    background = Slate50,
    onBackground = Slate900,
    surface = White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate200,
    error = StatusCrimson,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = HomeHelpGold,
    onPrimary = Slate900,
    primaryContainer = HomeHelpNavyLight,
    onPrimaryContainer = White,
    secondary = HomeHelpGold,
    onSecondary = Slate900,
    tertiary = HomeHelpCyan,
    background = HomeHelpNavyDark,
    onBackground = Slate50,
    surface = HomeHelpNavy,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200,
    outline = Slate700,
    error = StatusCrimson,
    onError = White
)

@Composable
fun HomeHelpPartnerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
