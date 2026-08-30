package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * Adaptive Glassmorphic Typography System:
 * Automatically scales font sizes based on user system display preferences (fontScale & screen width)
 * while maintaining glassmorphic container aesthetics, border ratios, and prevent text clipping.
 */
data class AdaptiveGlassTypography(
    val fontScale: Float,
    val isAccessibilityScale: Boolean,
    
    // Dynamic Typography Styles
    val heroTemperature: TextStyle,
    val heroUnit: TextStyle,
    val conditionHeadline: TextStyle,
    val sectionHeader: TextStyle,
    val metricValue: TextStyle,
    val metricLabel: TextStyle,
    val bodyBriefing: TextStyle,
    val badgeText: TextStyle,
    val timeLabel: TextStyle,
    val subText: TextStyle
)

/**
 * Calculates adaptive font sizes using non-linear scaling curves:
 * - Smaller text (captions, badges) scales generously for maximum readability.
 * - Massive display numbers (hero temperature) scale with optical boundaries to preserve glass card geometry.
 */
@Composable
@ReadOnlyComposable
fun rememberAdaptiveTypography(): AdaptiveGlassTypography {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val systemFontScale = density.fontScale
    val screenWidthDp = config.screenWidthDp

    // Non-linear scaling factor for massive display metrics (prevents hero temp clipping)
    val displayHeroScaleFactor = when {
        systemFontScale > 1.4f -> 1.18f
        systemFontScale > 1.2f -> 1.10f
        systemFontScale < 0.9f -> 0.92f
        else -> systemFontScale
    }

    // Adjust hero base size on narrow screens
    val heroBaseSize = if (screenWidthDp < 360) 72f else 84f
    val heroFontSize = (heroBaseSize * displayHeroScaleFactor).sp
    val heroUnitSize = (28f * min(displayHeroScaleFactor, 1.15f)).sp

    return AdaptiveGlassTypography(
        fontScale = systemFontScale,
        isAccessibilityScale = systemFontScale >= 1.25f,
        
        heroTemperature = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.ExtraLight,
            fontSize = heroFontSize,
            letterSpacing = (-2).sp
        ),
        heroUnit = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Light,
            fontSize = heroUnitSize
        ),
        conditionHeadline = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = (22 * min(systemFontScale, 1.3f)).sp,
            lineHeight = (28 * min(systemFontScale, 1.3f)).sp
        ),
        sectionHeader = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (12 * min(systemFontScale, 1.35f)).sp,
            letterSpacing = 0.8.sp,
            color = TextSecondary
        ),
        metricValue = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (23 * min(systemFontScale, 1.25f)).sp,
            lineHeight = (28 * min(systemFontScale, 1.25f)).sp,
            color = TextPrimary
        ),
        metricLabel = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = (10.5f * min(systemFontScale, 1.3f)).sp,
            letterSpacing = 0.6.sp,
            color = TextMuted
        ),
        bodyBriefing = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * systemFontScale).sp,
            lineHeight = (20 * systemFontScale).sp,
            color = TextPrimary
        ),
        badgeText = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = (11 * min(systemFontScale, 1.35f)).sp,
            letterSpacing = 0.4.sp
        ),
        timeLabel = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * min(systemFontScale, 1.3f)).sp,
            color = TextSecondary
        ),
        subText = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = (11 * min(systemFontScale, 1.35f)).sp,
            color = TextSecondary
        )
    )
}

val LocalGlassTypography = staticCompositionLocalOf<AdaptiveGlassTypography> {
    error("No AdaptiveGlassTypography provided")
}

// Default Material Typography
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    )
)

