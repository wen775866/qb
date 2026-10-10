package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = Color.Black,
    primaryContainer = ImperialRed,
    onPrimaryContainer = Color.White,
    secondary = BrightGold,
    onSecondary = Color.Black,
    secondaryContainer = WoodAccent,
    onSecondaryContainer = Color.White,
    tertiary = TableFeltLight,
    onTertiary = Color.White,
    background = SurfaceDark,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardElevated,
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = DarkColorScheme // Always maintain the atmospheric gaming aesthetics

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
