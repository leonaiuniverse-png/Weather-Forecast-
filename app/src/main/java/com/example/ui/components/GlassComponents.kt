package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SsidChart
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HourlyForecastItem
import com.example.data.model.SunCycleInfo
import com.example.data.model.WeatherConditionType
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentAmberGlow
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentCyanGlow
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.GlassBorderEnd
import com.example.ui.theme.GlassBorderStart
import com.example.ui.theme.GlassFillEnd
import com.example.ui.theme.GlassFillStart
import com.example.ui.theme.GradientClearDay
import com.example.ui.theme.GradientClearNight
import com.example.ui.theme.GradientFog
import com.example.ui.theme.GradientRain
import com.example.ui.theme.GradientSnow
import com.example.ui.theme.GradientSunset
import com.example.ui.theme.GradientThunderstorm
import com.example.ui.theme.LocalGlassTypography
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Reusable Glassmorphic Card Container:
 * - Border radius: 24.dp
 * - Linear Gradient fill: rgba(255,255,255,0.16) to rgba(255,255,255,0.03)
 * - Refractive edge border: 1.dp solid rgba(255,255,255,0.22)
 * - Ambient depth shadow
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderGradient: Brush = Brush.linearGradient(
        colors = listOf(GlassBorderStart, GlassBorderEnd)
    ),
    fillGradient: Brush = Brush.linearGradient(
        colors = listOf(GlassFillStart, GlassFillEnd),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    ),
    tintColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    val baseModifier = modifier
        .shadow(
            elevation = 14.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.35f),
            spotColor = Color.Black.copy(alpha = 0.45f)
        )
        .clip(shape)
        .background(fillGradient)
        .then(
            if (tintColor != null) {
                Modifier.background(tintColor.copy(alpha = 0.07f))
            } else {
                Modifier
            }
        )
        .border(
            border = BorderStroke(1.dp, borderGradient),
            shape = shape
        )
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )

    Box(
        modifier = baseModifier,
        content = content
    )
}

/**
 * Dynamic Atmospheric Background that shifts based on weather conditions.
 */
@Composable
fun AtmosphericBackground(
    conditionType: WeatherConditionType,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val gradientColors = when (conditionType) {
        WeatherConditionType.CLEAR_DAY -> GradientClearDay
        WeatherConditionType.CLEAR_NIGHT -> GradientClearNight
        WeatherConditionType.RAINY -> GradientRain
        WeatherConditionType.THUNDERSTORM -> GradientThunderstorm
        WeatherConditionType.SNOWY -> GradientSnow
        WeatherConditionType.FOGGY -> GradientFog
        WeatherConditionType.CLOUDY -> GradientRain
    }

    val infiniteTransition = rememberInfiniteTransition(label = "AtmosphereDrift")
    val driftOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AtmosphereDrift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Multi-stop base linear gradient
                drawRect(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                )

                // Atmospheric radial mesh glow 1 (Top right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            when (conditionType) {
                                WeatherConditionType.CLEAR_DAY -> AccentAmber.copy(alpha = 0.22f)
                                WeatherConditionType.THUNDERSTORM -> AccentCyan.copy(alpha = 0.25f)
                                WeatherConditionType.RAINY -> AccentCyan.copy(alpha = 0.16f)
                                else -> AccentCyan.copy(alpha = 0.14f)
                            },
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f + driftOffset, size.height * 0.15f),
                        radius = size.width * 0.65f
                    )
                )

                // Atmospheric radial mesh glow 2 (Bottom left)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            when (conditionType) {
                                WeatherConditionType.THUNDERSTORM -> Color(0xFF9333EA).copy(alpha = 0.25f)
                                WeatherConditionType.CLEAR_DAY -> AccentCyan.copy(alpha = 0.18f)
                                else -> Color(0xFF1E3A8A).copy(alpha = 0.22f)
                            },
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f - driftOffset, size.height * 0.75f),
                        radius = size.width * 0.75f
                    )
                )
            }
    ) {
        // Overlay particle animation
        WeatherParticleOverlay(conditionType = conditionType)
        
        // App Content
        content()
    }
}

/**
 * Pulsing Sparkle AI Indicator.
 */
@Composable
fun PulsingSparkle(
    modifier: Modifier = Modifier,
    color: Color = AccentCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SparklePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkleScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkleGlow"
    )

    Box(
        modifier = modifier.size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer halo
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .background(
                    color = color.copy(alpha = glowAlpha * 0.4f),
                    shape = CircleShape
                )
        )
        // Icon
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = "AI Intelligence Indicator",
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * 3D-styled animated hero weather condition icon.
 */
@Composable
fun WeatherCondition3DIcon(
    weatherCode: Int,
    isDay: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WeatherFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatOffset"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val (iconVector, primaryColor, glowColor) = getWeatherIconData(weatherCode, isDay)

    Box(
        modifier = modifier
            .size(96.dp)
            .offset(y = floatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ambient backlight glow disc
        Box(
            modifier = Modifier
                .size(76.dp)
                .scale(pulseScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        // Main Icon
        Icon(
            imageVector = iconVector,
            contentDescription = "Current weather icon",
            tint = primaryColor,
            modifier = Modifier
                .size(76.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = primaryColor, spotColor = primaryColor)
        )
    }
}

fun getWeatherIconData(weatherCode: Int, isDay: Boolean): Triple<ImageVector, Color, Color> {
    return when (weatherCode) {
        0, 1 -> if (isDay) {
            Triple(Icons.Rounded.WbSunny, AccentAmber, AccentAmberGlow)
        } else {
            Triple(Icons.Rounded.NightsStay, Color(0xFFC7D2FE), Color(0x66818CF8))
        }
        2, 3 -> Triple(Icons.Rounded.Cloud, Color(0xFFE2E8F0), Color(0x4494A3B8))
        45, 48 -> Triple(Icons.Rounded.Air, Color(0xFFCBD5E1), Color(0x3364748B))
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> Triple(Icons.Rounded.Grain, AccentCyan, AccentCyanGlow)
        71, 73, 75, 77, 85, 86 -> Triple(Icons.Rounded.AcUnit, Color(0xFFE0F2FE), Color(0x6638BDF8))
        95, 96, 99 -> Triple(Icons.Rounded.FlashOn, AccentAmber, Color(0x88F59E0B))
        else -> Triple(Icons.Rounded.WbCloudy, Color(0xFFE2E8F0), Color(0x4494A3B8))
    }
}

/**
 * Min-Max Temperature Horizontal Bar with continuous gradient.
 */
@Composable
fun MinMaxThermometerBar(
    minTemp: Double,
    maxTemp: Double,
    weekMin: Double,
    weekMax: Double,
    modifier: Modifier = Modifier
) {
    val totalRange = (weekMax - weekMin).coerceAtLeast(1.0)
    val startFraction = ((minTemp - weekMin) / totalRange).toFloat().coerceIn(0f, 0.9f)
    val endFraction = ((maxTemp - weekMin) / totalRange).toFloat().coerceIn(startFraction + 0.1f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
    ) {
        val barWidth = size.width
        val barHeight = size.height

        // Background track (dark translucent)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.12f),
            size = Size(barWidth, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2, barHeight / 2)
        )

        // Active range gradient
        val activeStartX = barWidth * startFraction
        val activeEndX = barWidth * endFraction
        val activeWidth = (activeEndX - activeStartX).coerceAtLeast(barHeight)

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(AccentCyan, AccentAmber, AccentRose),
                startX = activeStartX,
                endX = activeEndX
            ),
            topLeft = Offset(activeStartX, 0f),
            size = Size(activeWidth, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2, barHeight / 2)
        )
    }
}

/**
 * Directional Compass Rose for Wind speed card.
 */
@Composable
fun WindCompassRose(
    speedKmh: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .background(Color.White.copy(alpha = 0.08f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Explore,
            contentDescription = "Wind Compass",
            tint = AccentCyan,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Moisture Wave Indicator for Humidity card.
 */
@Composable
fun MoistureWaveIndicator(
    humidityPct: Int,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val width = size.width
        val height = size.height
        val fraction = (humidityPct / 100f).coerceIn(0f, 1f)

        // Background line
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Progress bar with cyan glow
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(AccentCyan.copy(alpha = 0.4f), AccentCyan)
            ),
            start = Offset(0f, height / 2),
            end = Offset(width * fraction, height / 2),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Wave bead
        drawCircle(
            color = AccentCyan,
            radius = 5.dp.toPx(),
            center = Offset(width * fraction, height / 2)
        )
    }
}

/**
 * UV Index gauge bar.
 */
@Composable
fun UvGaugeBar(
    uvIndex: Double,
    modifier: Modifier = Modifier
) {
    val fraction = (uvIndex / 12.0).toFloat().coerceIn(0f, 1f)
    val uvColor = when {
        uvIndex < 3.0 -> AccentEmerald
        uvIndex < 6.0 -> AccentAmber
        uvIndex < 8.0 -> Color(0xFFF97316)
        else -> Color(0xFFA855F7)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        ) {
            val width = size.width
            val height = size.height

            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                size = Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2, height / 2)
            )

            drawRoundRect(
                color = uvColor,
                size = Size((width * fraction).coerceAtLeast(height), height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2, height / 2)
            )
        }
    }
}

/**
 * Animated Particle System overlay for weather effects:
 * - Rain streaks with diagonal slant and specular glow
 * - Soft drifting snowflakes with sinusoidal sway
 * - Drifting cloud puffs
 * - Lightning flash on thunderstorms
 * - Twinkling starlight on clear nights
 */
@Composable
fun WeatherParticleOverlay(
    conditionType: WeatherConditionType,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WeatherParticles")
    
    // Continuous progress loop 0f -> 1f
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ParticleProgress"
    )

    // Lightning flash animation for thunderstorms
    val lightningAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LightningAlpha"
    )

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        when (conditionType) {
            WeatherConditionType.RAINY, WeatherConditionType.THUNDERSTORM -> {
                // Lightning flash for thunderstorms
                if (conditionType == WeatherConditionType.THUNDERSTORM && lightningAlpha > 0.88f) {
                    val flashStrength = (lightningAlpha - 0.88f) / 0.12f
                    drawRect(
                        color = Color.White.copy(alpha = flashStrength * 0.15f)
                    )
                }

                // Render 40 dynamic rain streaks
                val rainCount = if (conditionType == WeatherConditionType.THUNDERSTORM) 50 else 32
                for (i in 0 until rainCount) {
                    val seed = i * 137.5f
                    val speedFactor = 0.8f + ((seed % 10) / 10f) * 0.8f
                    val streakLength = 35f + (seed % 25)
                    val startX = (seed * 31.7f) % width
                    val currentY = ((progress * speedFactor + (seed / 100f)) % 1f) * (height + streakLength * 2) - streakLength
                    val currentX = startX + (currentY * 0.12f) // Wind angle slant

                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AccentCyan.copy(alpha = 0.05f),
                                AccentCyan.copy(alpha = 0.45f)
                            ),
                            startY = currentY,
                            endY = currentY + streakLength
                        ),
                        start = Offset(currentX, currentY),
                        end = Offset(currentX + streakLength * 0.12f, currentY + streakLength),
                        strokeWidth = 2.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            WeatherConditionType.SNOWY -> {
                // Render 35 soft drifting snowflakes
                for (i in 0 until 35) {
                    val seed = i * 97.3f
                    val speedFactor = 0.3f + ((seed % 8) / 8f) * 0.4f
                    val radius = 2.dp.toPx() + ((seed % 5) * 0.8f).dp.toPx()
                    val baseX = (seed * 43.1f) % width
                    val currentY = ((progress * speedFactor + (seed / 100f)) % 1f) * (height + 20f)
                    val sway = kotlin.math.sin((progress * 6.28f * speedFactor) + seed).toFloat() * 24.dp.toPx()
                    val currentX = (baseX + sway) % width

                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f + ((seed % 4) * 0.1f)),
                        radius = radius,
                        center = Offset(currentX, currentY)
                    )
                }
            }

            WeatherConditionType.CLOUDY, WeatherConditionType.FOGGY -> {
                // Render drifting soft atmospheric mist puffs
                for (i in 0 until 4) {
                    val seed = i * 211.1f
                    val speedFactor = 0.08f + (i * 0.04f)
                    val cloudRadius = width * 0.45f
                    val currentX = ((progress * speedFactor + (seed / 100f)) % 1f) * (width + cloudRadius * 2) - cloudRadius
                    val currentY = height * 0.12f + (i * 60.dp.toPx())

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(currentX, currentY),
                            radius = cloudRadius
                        ),
                        radius = cloudRadius,
                        center = Offset(currentX, currentY)
                    )
                }
            }

            WeatherConditionType.CLEAR_NIGHT -> {
                // Render twinkling night stars
                for (i in 0 until 28) {
                    val seed = i * 79.7f
                    val starX = (seed * 53.3f) % width
                    val starY = (seed * 29.1f) % (height * 0.65f)
                    val twinkle = (kotlin.math.sin(progress * 6.28f * (1f + (i % 3)) + seed).toFloat() + 1f) / 2f
                    val starRadius = (1.5f + (twinkle * 1.5f)).dp.toPx()

                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f + twinkle * 0.7f),
                        radius = starRadius,
                        center = Offset(starX, starY)
                    )
                }
            }

            WeatherConditionType.CLEAR_DAY -> {
                // Render subtle ambient sun shimmer
                val shimmerRadius = width * 0.8f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentAmber.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.15f),
                        radius = shimmerRadius
                    ),
                    radius = shimmerRadius,
                    center = Offset(width * 0.85f, height * 0.15f)
                )
            }
        }
    }
}

/**
 * Sun Cycle Glass Card:
 * Displays real-time celestial solar trajectory, sunrise/sunset times, daylight length,
 * and time until the next solar transition.
 */
@Composable
fun SunCycleCard(
    sunCycle: SunCycleInfo,
    modifier: Modifier = Modifier
) {
    val typo = LocalGlassTypography.current

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sun_cycle_card"),
        cornerRadius = 24.dp,
        borderGradient = Brush.linearGradient(
            colors = listOf(
                AccentAmber.copy(alpha = 0.35f),
                GlassBorderEnd
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Icon + Title + Solar Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(AccentAmber.copy(alpha = 0.16f), CircleShape)
                        .border(1.dp, AccentAmber.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = "Sun Cycle",
                        tint = AccentAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "SUN CYCLE",
                    style = typo.sectionHeader,
                    color = AccentAmber
                )

                Spacer(modifier = Modifier.weight(1f))

                // Status Badge (e.g., "Sunset in 54m" or "Daylight 13h 14m")
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    AccentAmber.copy(alpha = 0.18f),
                                    AccentAmber.copy(alpha = 0.08f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            AccentAmber.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = sunCycle.solarStatus,
                        style = typo.badgeText,
                        color = AccentAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Celestial Arc Visualization
            SunCycleArc(
                progress = sunCycle.solarProgress,
                isDaytime = sunCycle.isDaytime,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Metrics Row: Sunrise, Daylight Duration, Sunset
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sunrise
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AccentAmber, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "SUNRISE",
                            style = typo.metricLabel
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sunCycle.sunriseFormatted,
                        style = typo.timeLabel.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                        fontSize = (15 * min(typo.fontScale, 1.25f)).sp
                    )
                    Text(
                        text = "Dawn",
                        style = typo.subText
                    )
                }

                // Daylight Duration Center Pill
                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "DAYLIGHT",
                                style = typo.metricLabel.copy(fontSize = (9 * min(typo.fontScale, 1.25f)).sp)
                            )
                            Text(
                                text = sunCycle.daylightDurationFormatted,
                                style = typo.badgeText.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Sunset
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SUNSET",
                            style = typo.metricLabel
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AccentRose, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sunCycle.sunsetFormatted,
                        style = typo.timeLabel.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                        fontSize = (15 * min(typo.fontScale, 1.25f)).sp
                    )
                    Text(
                        text = "Dusk",
                        style = typo.subText
                    )
                }
            }
        }
    }
}

/**
 * Custom Canvas drawing the celestial solar arc with horizon baseline and sun progress.
 */
@Composable
fun SunCycleArc(
    progress: Float, // 0.0 (sunrise) to 1.0 (sunset)
    isDaytime: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val marginX = 24.dp.toPx()
        val horizonY = height - 14.dp.toPx()
        val arcWidth = width - (marginX * 2)
        val arcHeight = horizonY - 10.dp.toPx()

        // 1. Horizon Line
        drawLine(
            color = Color.White.copy(alpha = 0.14f),
            start = Offset(marginX - 12.dp.toPx(), horizonY),
            end = Offset(width - marginX + 12.dp.toPx(), horizonY),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 2. Solar Path Curve (Parabolic Arc)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(marginX, horizonY)
            // Cubic bezier for smooth bell-curve celestial trajectory
            cubicTo(
                marginX + arcWidth * 0.25f, horizonY - arcHeight * 1.15f,
                marginX + arcWidth * 0.75f, horizonY - arcHeight * 1.15f,
                marginX + arcWidth, horizonY
            )
        }

        // Draw complete background solar trajectory path (translucent dashed/subtle)
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.2f),
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // 3. Active Daylight Illuminated Arc Path
        if (progress > 0f) {
            val clampedProg = progress.coerceIn(0f, 1f)
            val fillPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(marginX, horizonY)
                val steps = 30
                val activeSteps = (steps * clampedProg).toInt().coerceAtLeast(1)
                for (s in 1..activeSteps) {
                    val t = (s.toFloat() / steps) * clampedProg
                    // Parabolic curve: y = 4 * arcHeight * t * (1 - t)
                    val px = marginX + (arcWidth * t)
                    val py = horizonY - (4f * arcHeight * t * (1f - t))
                    lineTo(px, py)
                }
                lineTo(marginX + (arcWidth * clampedProg), horizonY)
                close()
            }

            // Glow under the illuminated curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AccentAmber.copy(alpha = 0.22f),
                        AccentAmber.copy(alpha = 0.02f)
                    ),
                    startY = horizonY - arcHeight,
                    endY = horizonY
                )
            )
        }

        // 4. Calculate Sun position along parabolic trajectory
        val currentT = if (isDaytime) progress.coerceIn(0.02f, 0.98f) else if (progress <= 0f) 0.02f else 0.98f
        val sunX = marginX + (arcWidth * currentT)
        val sunY = horizonY - (4f * arcHeight * currentT * (1f - currentT))

        // 5. Sun Glow & Core Marker
        if (isDaytime) {
            // Sun Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentAmber.copy(alpha = 0.85f),
                        AccentAmber.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(sunX, sunY),
                    radius = 16.dp.toPx()
                ),
                radius = 16.dp.toPx(),
                center = Offset(sunX, sunY)
            )

            // Sun Core
            drawCircle(
                color = AccentAmber,
                radius = 5.5.dp.toPx(),
                center = Offset(sunX, sunY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = Offset(sunX, sunY)
            )
        } else {
            // Night Moon indicator
            drawCircle(
                color = Color(0xFFC7D2FE).copy(alpha = 0.6f),
                radius = 4.5.dp.toPx(),
                center = Offset(sunX, sunY)
            )
        }

        // 6. Horizon Node Dots (Sunrise & Sunset points)
        drawCircle(
            color = AccentAmber,
            radius = 3.dp.toPx(),
            center = Offset(marginX, horizonY)
        )
        drawCircle(
            color = AccentRose,
            radius = 3.dp.toPx(),
            center = Offset(marginX + arcWidth, horizonY)
        )
    }
}

/**
 * 6. Feels Like Temperature Trend Card
 * Visualizes the 24-hour apparent temperature curve with a smooth cubic Bézier canvas graph,
 * glowing ambient accents, peak/low delta markers, and real-time comparative insights.
 */
@Composable
fun FeelsLikeTrendCard(
    hourlyList: List<HourlyForecastItem>,
    currentTempC: Double,
    apparentTempC: Double,
    convertTemp: (Double) -> Int,
    unitSymbol: String,
    modifier: Modifier = Modifier
) {
    val typo = LocalGlassTypography.current
    val items = hourlyList.take(24)
    if (items.isEmpty()) return

    val currentConvertedApparent = convertTemp(apparentTempC)
    val currentConvertedActual = convertTemp(currentTempC)
    val tempDiff = currentConvertedApparent - currentConvertedActual

    val apparentTemps = items.map { convertTemp(it.apparentTempC) }
    val maxApparent = apparentTemps.maxOrNull() ?: currentConvertedApparent
    val minApparent = apparentTemps.minOrNull() ?: currentConvertedApparent

    val peakItem = items.maxByOrNull { it.apparentTempC }
    val lowItem = items.minByOrNull { it.apparentTempC }

    val deltaBadgeText = when {
        tempDiff > 0 -> "+$tempDiff° vs Actual"
        tempDiff < 0 -> "$tempDiff° vs Actual"
        else -> "Matches Actual"
    }

    val deltaBadgeColor = when {
        tempDiff > 0 -> AccentAmber
        tempDiff < 0 -> AccentCyan
        else -> TextSecondary
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("feels_like_trend_card"),
        cornerRadius = 24.dp,
        borderGradient = Brush.verticalGradient(
            listOf(
                AccentCyan.copy(alpha = 0.45f),
                GlassBorderEnd
            )
        ),
        fillGradient = Brush.verticalGradient(
            listOf(
                Color(0x220EA5E9),
                Color(0x0A0F172A)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Section title, icon, and dynamic delta insight badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(AccentCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SsidChart,
                            contentDescription = "Feels Like Trend",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "FEELS LIKE TREND",
                            style = typo.sectionHeader,
                            color = AccentCyan
                        )
                        Text(
                            text = "24-Hour Thermal Profile",
                            style = typo.subText.copy(fontSize = (10.5f * min(typo.fontScale, 1.25f)).sp)
                        )
                    }
                }

                // Comparative Delta Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(deltaBadgeColor.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            deltaBadgeColor.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = deltaBadgeText,
                        color = deltaBadgeColor,
                        style = typo.badgeText.copy(fontSize = (10f * min(typo.fontScale, 1.25f)).sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick High / Low / Current summary metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current Feels Like
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(AccentCyan, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Now: $currentConvertedApparent$unitSymbol",
                        style = typo.timeLabel.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = (12.5f * min(typo.fontScale, 1.2f)).sp
                        )
                    )
                }

                // Peak Feels Like
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(AccentAmber, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "High: $maxApparent$unitSymbol",
                        style = typo.timeLabel.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AccentAmber,
                            fontSize = (12f * min(typo.fontScale, 1.2f)).sp
                        )
                    )
                }

                // Low Feels Like
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF818CF8), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Low: $minApparent$unitSymbol",
                        style = typo.timeLabel.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC7D2FE),
                            fontSize = (12f * min(typo.fontScale, 1.2f)).sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Smooth Canvas Line Graph
            FeelsLikeLineChart(
                items = items,
                apparentTemps = apparentTemps,
                convertTemp = convertTemp,
                unitSymbol = unitSymbol,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .testTag("feels_like_canvas_chart")
            )
        }
    }
}

/**
 * Custom Canvas drawing a smooth cubic Bézier line graph for 24-hour feels-like trend,
 * complete with gradient atmospheric under-fill, pulsating current-time indicator,
 * horizontal dashed reference lines, and formatted time axis labels.
 */
@Composable
fun FeelsLikeLineChart(
    items: List<HourlyForecastItem>,
    apparentTemps: List<Int>,
    convertTemp: (Double) -> Int,
    unitSymbol: String,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty() || apparentTemps.isEmpty()) return

    val infiniteTransition = rememberInfiniteTransition(label = "feels_like_pulse")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val minVal = apparentTemps.minOrNull() ?: 0
    val maxVal = apparentTemps.maxOrNull() ?: (minVal + 1)
    val rangeSpan = max(1f, (maxVal - minVal).toFloat())

    val textPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val leftMargin = 10.dp.toPx()
        val rightMargin = 34.dp.toPx()
        val topMargin = 20.dp.toPx()
        val bottomMargin = 22.dp.toPx()

        val chartWidth = width - leftMargin - rightMargin
        val chartHeight = height - topMargin - bottomMargin

        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        // 1. Horizontal Reference Dashed Grid Lines & Temperature Labels
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()), 0f)
        val gridColor = Color.White.copy(alpha = 0.10f)

        // Max guide line
        drawLine(
            color = gridColor,
            start = Offset(leftMargin, topMargin),
            end = Offset(width - rightMargin, topMargin),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect
        )

        // Mid guide line
        val midY = topMargin + chartHeight / 2f
        drawLine(
            color = gridColor,
            start = Offset(leftMargin, midY),
            end = Offset(width - rightMargin, midY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect
        )

        // Min guide line
        val baselineY = topMargin + chartHeight
        drawLine(
            color = gridColor,
            start = Offset(leftMargin, baselineY),
            end = Offset(width - rightMargin, baselineY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dashEffect
        )

        // Reference temperature scale labels on the right
        val nativeCanvas = drawContext.canvas.nativeCanvas
        textPaint.textSize = 9.5f.sp.toPx()
        textPaint.textAlign = android.graphics.Paint.Align.LEFT
        textPaint.color = android.graphics.Color.argb(170, 148, 163, 184) // Slate 400

        nativeCanvas.drawText("${maxVal}°", width - rightMargin + 6.dp.toPx(), topMargin + 3.dp.toPx(), textPaint)
        val midTemp = (maxVal + minVal) / 2
        nativeCanvas.drawText("${midTemp}°", width - rightMargin + 6.dp.toPx(), midY + 3.dp.toPx(), textPaint)
        nativeCanvas.drawText("${minVal}°", width - rightMargin + 6.dp.toPx(), baselineY + 3.dp.toPx(), textPaint)

        // 2. Calculate coordinates for all 24 data points
        val pointCount = items.size
        val points = items.indices.map { i ->
            val x = if (pointCount > 1) {
                leftMargin + (i.toFloat() / (pointCount - 1)) * chartWidth
            } else {
                leftMargin + chartWidth / 2f
            }
            val norm = (apparentTemps[i] - minVal) / rangeSpan
            val y = topMargin + (1f - norm) * chartHeight
            Offset(x, y)
        }

        // 3. Build Smooth Cubic Bézier Spline Path
        val strokePath = Path()
        val fillPath = Path()

        if (points.isNotEmpty()) {
            strokePath.moveTo(points.first().x, points.first().y)
            fillPath.moveTo(points.first().x, baselineY)
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val cx1 = prev.x + (curr.x - prev.x) / 2f
                val cy1 = prev.y
                val cx2 = prev.x + (curr.x - prev.x) / 2f
                val cy2 = curr.y

                strokePath.cubicTo(cx1, cy1, cx2, cy2, curr.x, curr.y)
                fillPath.cubicTo(cx1, cy1, cx2, cy2, curr.x, curr.y)
            }

            fillPath.lineTo(points.last().x, baselineY)
            fillPath.close()

            // 4. Fill atmospheric gradient under the line
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AccentCyan.copy(alpha = 0.32f),
                        Color(0xFF6366F1).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    startY = topMargin,
                    endY = baselineY
                )
            )

            // 5. Outer ambient neon glow stroke
            drawPath(
                path = strokePath,
                brush = Brush.horizontalGradient(
                    listOf(
                        AccentCyan.copy(alpha = 0.35f),
                        AccentAmber.copy(alpha = 0.35f),
                        Color(0xFF818CF8).copy(alpha = 0.35f)
                    )
                ),
                style = Stroke(
                    width = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 6. Main crisp curve stroke
            drawPath(
                path = strokePath,
                brush = Brush.horizontalGradient(
                    listOf(
                        AccentCyan,
                        AccentAmber,
                        Color(0xFFA5B4FC)
                    )
                ),
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // 7. Peak & Low Marker Accents
        val peakIndex = apparentTemps.indexOf(maxVal).takeIf { it >= 0 }
        val lowIndex = apparentTemps.indexOf(minVal).takeIf { it >= 0 }

        if (peakIndex != null && peakIndex in points.indices && points.size > 2) {
            val peakPt = points[peakIndex]
            drawCircle(
                color = AccentAmber,
                radius = 3.5.dp.toPx(),
                center = peakPt
            )
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = peakPt
            )
            // Label above peak
            textPaint.textAlign = android.graphics.Paint.Align.CENTER
            textPaint.color = AccentAmber.toArgb()
            textPaint.textSize = 9.sp.toPx()
            val labelX = peakPt.x.coerceIn(leftMargin + 20.dp.toPx(), width - rightMargin - 20.dp.toPx())
            nativeCanvas.drawText("Peak ${maxVal}°", labelX, peakPt.y - 7.dp.toPx(), textPaint)
        }

        if (lowIndex != null && lowIndex in points.indices && lowIndex != peakIndex && points.size > 2) {
            val lowPt = points[lowIndex]
            drawCircle(
                color = Color(0xFF818CF8),
                radius = 3.dp.toPx(),
                center = lowPt
            )
        }

        // 8. Active Current-Hour Glowing Indicator
        val nowIndex = items.indexOfFirst { it.isCurrentHour }.takeIf { it >= 0 } ?: 0
        if (nowIndex in points.indices) {
            val nowPt = points[nowIndex]

            // Subtle vertical drop line to baseline
            drawLine(
                color = AccentCyan.copy(alpha = 0.35f),
                start = Offset(nowPt.x, nowPt.y),
                end = Offset(nowPt.x, baselineY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
            )

            // Outer pulsating aura
            drawCircle(
                color = AccentCyan.copy(alpha = 0.35f * pulseAnim),
                radius = (10f + 4f * pulseAnim).dp.toPx(),
                center = nowPt
            )

            // Inner solid glowing node
            drawCircle(
                color = AccentCyan,
                radius = 4.5.dp.toPx(),
                center = nowPt
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = nowPt
            )
        }

        // 9. Bottom X-Axis Time Ticks
        textPaint.textSize = 9.5f.sp.toPx()
        textPaint.color = android.graphics.Color.argb(185, 148, 163, 184)

        val tickIndices = listOf(
            0,
            (pointCount * 0.25f).toInt(),
            (pointCount * 0.5f).toInt(),
            (pointCount * 0.75f).toInt(),
            pointCount - 1
        ).distinct()

        for (idx in tickIndices) {
            if (idx in items.indices) {
                val pt = points[idx]
                val label = items[idx].hourLabel
                val align = when (idx) {
                    0 -> android.graphics.Paint.Align.LEFT
                    pointCount - 1 -> android.graphics.Paint.Align.RIGHT
                    else -> android.graphics.Paint.Align.CENTER
                }
                textPaint.textAlign = align
                nativeCanvas.drawText(label, pt.x, height - 3.dp.toPx(), textPaint)
            }
        }
    }
}

/**
 * 7. Glassmorphic Share Floating Action Button
 * Provides a tactile floating action pill with glowing cyan/amber gradient aura,
 * glass reflection styling, and an accessibility touch target >= 56dp.
 */
@Composable
fun GlassShareFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typo = LocalGlassTypography.current

    val infiniteTransition = rememberInfiniteTransition(label = "share_fab_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_glow_alpha"
    )

    Box(
        modifier = modifier
            .testTag("share_weather_fab")
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AccentCyan.copy(alpha = 0.28f * glowAlpha),
                        Color(0xD90F172A)
                    ),
                    radius = 180f
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        AccentCyan.copy(alpha = 0.85f * glowAlpha),
                        AccentAmber.copy(alpha = 0.45f * glowAlpha),
                        GlassBorderEnd
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(AccentCyan, Color(0xFF0284C7))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Share Weather Dashboard",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Share Sky",
                color = TextPrimary,
                style = typo.badgeText.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (13f * min(typo.fontScale, 1.25f)).sp,
                    letterSpacing = 0.6.sp
                )
            )
        }
    }
}


