package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0x3300F2FE),
    onPrimaryContainer = Color.White,
    secondary = AccentAmber,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0x33FFD200),
    onSecondaryContainer = Color.White,
    tertiary = AccentEmerald,
    background = Color(0xFF0F172A),
    onBackground = Color.White,
    surface = Color(0x1AFFFFFF),
    onSurface = Color.White,
    surfaceVariant = Color(0x26FFFFFF),
    onSurfaceVariant = Color.White,
    outline = Color(0x38FFFFFF)
)

@Composable
fun SkyGlassWeatherTheme(
    content: @Composable () -> Unit
) {
    val adaptiveTypography = rememberAdaptiveTypography()

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
    ) {
        CompositionLocalProvider(
            LocalGlassTypography provides adaptiveTypography
        ) {
            content()
        }
    }
}

