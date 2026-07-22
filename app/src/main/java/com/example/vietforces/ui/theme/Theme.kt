package com.example.vietforces.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VietRed,
    onPrimary = Color.White,
    secondary = VietYellow,
    onSecondary = Color.Black,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = VietRed,
    onPrimary = Color.White,
    secondary = VietYellow,
    onSecondary = Color.Black,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = TextSecondary
)

@Composable
fun VietforcesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // dynamicColor defaults to false to preserve VietRed/VietYellow brand colors on all API levels.
    // Setting true on Android 12+ would override brand colors with system wallpaper colors (Material You).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}