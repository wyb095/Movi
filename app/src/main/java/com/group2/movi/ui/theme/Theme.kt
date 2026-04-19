package com.group2.movi.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PrimaryContainerLight = Color(0xFFD6EAF8)

private val LightColors = lightColorScheme(
    primary = MoviPrimary,
    onPrimary = MoviSurface,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = MoviPrimary,
    secondary = MoviSecondary,
    onSecondary = MoviSurface,
    tertiary = MoviAccent,
    onTertiary = MoviSurface,
    error = MoviError,
    background = MoviBackground,
    onBackground = MoviOnSurface,
    surface = MoviSurface,
    onSurface = MoviOnSurface,
    onSurfaceVariant = MoviOnSurfaceVariant,
    outline = MoviOutline
)

private val DarkColors = darkColorScheme(
    primary = MoviPrimaryDark,
    onPrimary = MoviSurface,
    secondary = MoviSecondary,
    tertiary = MoviAccent,
    error = MoviError,
    background = MoviBackgroundDark,
    onBackground = MoviSurface,
    surface = MoviSurfaceDark,
    onSurface = MoviSurface
)

@Composable
fun MoviTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

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
        typography = MoviTypography,
        content = content
    )
}
