package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimaryDark,
    onPrimary = Color(0xFF003554),
    primaryContainer = CyanPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = AmberSecondaryDark,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = AmberSecondaryContainerDark,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = EmeraldTertiaryDark,
    background = SlateDarkBackground,
    surface = SlateDarkSurface,
    surfaceVariant = SlateDarkCard,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    outline = SlateDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.White,
    primaryContainer = CyanPrimaryContainer,
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = AmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = EmeraldTertiary,
    background = SlateLightBackground,
    surface = SlateLightSurface,
    surfaceVariant = SlateLightCard,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    outline = SlateLightBorder
)

@Composable
fun ECellTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
