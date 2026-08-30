package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Brightness5
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Brightness7
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.SunCycleInfo
import com.example.data.model.WeatherConditionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Diurnal Solar Phases mapped to distinct celestial and atmospheric lighting intervals
 * determined by local sunrise, solar zenith, sunset, and twilight epochs.
 */
enum class DiurnalSolarPhase(
    val title: String,
    val shortLabel: String,
    val moodDescription: String,
    val isDaytime: Boolean
) {
    DEEP_NIGHT(
        title = "Deep Starlight Night",
        shortLabel = "Starlight Night",
        moodDescription = "Cosmic obsidian & deep indigo starlight",
        isDaytime = false
    ),
    DAWN_TWILIGHT(
        title = "Blue Hour Dawn",
        shortLabel = "Dawn Twilight",
        moodDescription = "Pre-sunrise indigo & electric violet veil",
        isDaytime = false
    ),
    GOLDEN_SUNRISE(
        title = "Golden Hour Sunrise",
        shortLabel = "Golden Sunrise",
        moodDescription = "Radiant amber gold, peach & rose horizon",
        isDaytime = true
    ),
    MORNING_AZURE(
        title = "Crisp Morning Azure",
        shortLabel = "Morning Azure",
        moodDescription = "Luminous clear sky azure & fresh cyan",
        isDaytime = true
    ),
    SOLAR_NOON(
        title = "Solar Noon Zenith",
        shortLabel = "Solar Zenith",
        moodDescription = "High-sun ultramarine & brilliant solar glow",
        isDaytime = true
    ),
    AFTERNOON_WARMTH(
        title = "Golden Afternoon",
        shortLabel = "Afternoon",
        moodDescription = "Warm cerulean & honeyed solar rays",
        isDaytime = true
    ),
    GOLDEN_SUNSET(
        title = "Golden Hour Sunset",
        shortLabel = "Golden Sunset",
        moodDescription = "Fiery crimson, sunset gold & magenta glow",
        isDaytime = true
    ),
    DUSK_TWILIGHT(
        title = "Blue Hour Dusk",
        shortLabel = "Dusk Twilight",
        moodDescription = "Post-sunset velvet twilight & amethyst dusk",
        isDaytime = false
    );

    val icon: ImageVector
        get() = when (this) {
            DEEP_NIGHT -> Icons.Rounded.NightsStay
            DAWN_TWILIGHT -> Icons.Rounded.Brightness4
            GOLDEN_SUNRISE -> Icons.Rounded.Brightness5
            MORNING_AZURE -> Icons.Rounded.LightMode
            SOLAR_NOON -> Icons.Rounded.WbSunny
            AFTERNOON_WARMTH -> Icons.Rounded.Brightness7
            GOLDEN_SUNSET -> Icons.Rounded.Brightness6
            DUSK_TWILIGHT -> Icons.Rounded.Bedtime
        }
}

/**
 * Complete UI color palette generated dynamically for a diurnal phase
 * and blended with ambient atmospheric weather conditions.
 */
data class DiurnalPalette(
    val phase: DiurnalSolarPhase,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val tertiaryAccent: Color,
    val primaryGlow: Color,
    val gradientStops: List<Color>, // 4-stop base atmospheric gradient
    val celestialGlow1: Color,      // Top-right celestial source glow
    val celestialGlow2: Color,      // Bottom-left tropospheric refraction
    val horizonGlow: Color,         // Mid-screen horizon diffusion glow
    val glassFillStart: Color,
    val glassFillEnd: Color,
    val glassBorderStart: Color,
    val glassBorderEnd: Color,
    val cardHighlight: Color,
    val badgeBackground: Color,
    val chipBorder: Color
)

/**
 * State container for the active diurnal theme scheduler
 */
data class DiurnalThemeState(
    val phase: DiurnalSolarPhase = DiurnalSolarPhase.MORNING_AZURE,
    val solarProgress: Float = 0.5f,
    val nextPhase: DiurnalSolarPhase = DiurnalSolarPhase.SOLAR_NOON,
    val nextTransitionSummary: String = "Solar Noon in 1h 45m",
    val isAutoScheduleEnabled: Boolean = true,
    val palette: DiurnalPalette = SolarThemeScheduler.generatePalette(DiurnalSolarPhase.MORNING_AZURE, WeatherConditionType.CLEAR_DAY, isDay = true)
)

val LocalDiurnalPalette = compositionLocalOf {
    SolarThemeScheduler.generatePalette(DiurnalSolarPhase.MORNING_AZURE, WeatherConditionType.CLEAR_DAY, isDay = true)
}

val LocalDiurnalThemeState = compositionLocalOf {
    DiurnalThemeState()
}

/**
 * Engine that computes the exact diurnal cycle based on sunrise/sunset timestamps
 * and builds tailored color palettes.
 */
object SolarThemeScheduler {

    /**
     * Evaluates current local timestamp against calculated sunrise and sunset times
     * to determine the exact DiurnalSolarPhase.
     */
    fun calculateDiurnalPhase(
        sunCycle: SunCycleInfo?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): DiurnalSolarPhase {
        if (sunCycle == null || sunCycle.sunriseIso.isBlank() || sunCycle.sunsetIso.isBlank()) {
            return fallbackPhaseFromLocalClock(currentTimeMillis)
        }

        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val sunriseDate = try { isoParser.parse(sunCycle.sunriseIso) } catch (e: Exception) { null }
        val sunsetDate = try { isoParser.parse(sunCycle.sunsetIso) } catch (e: Exception) { null }

        if (sunriseDate == null || sunsetDate == null) {
            return fallbackPhaseFromLocalClock(currentTimeMillis)
        }

        val sunriseMillis = sunriseDate.time
        val sunsetMillis = sunsetDate.time

        // Solar Noon approximation: midpoint between sunrise and sunset
        val solarNoonMillis = (sunriseMillis + sunsetMillis) / 2L
        val daylightDurationMillis = sunsetMillis - sunriseMillis

        // Epoch threshold deltas:
        // Dawn twilight: 60m before sunrise to 20m before sunrise
        // Golden hour sunrise: 20m before sunrise to 45m after sunrise
        // Morning azure: 45m after sunrise to (solarNoon - 75m)
        // Solar noon zenith: (solarNoon - 75m) to (solarNoon + 75m)
        // Afternoon warmth: (solarNoon + 75m) to 50m before sunset
        // Golden hour sunset: 50m before sunset to 20m after sunset
        // Dusk twilight: 20m after sunset to 75m after sunset
        // Deep night: remainder

        val minMs = 60 * 1000L
        val dawnStart = sunriseMillis - 65 * minMs
        val sunriseStart = sunriseMillis - 20 * minMs
        val sunriseEnd = sunriseMillis + 50 * minMs

        val noonStart = solarNoonMillis - 75 * minMs
        val noonEnd = solarNoonMillis + 75 * minMs

        val sunsetStart = sunsetMillis - 55 * minMs
        val sunsetEnd = sunsetMillis + 25 * minMs
        val duskEnd = sunsetMillis + 80 * minMs

        return when {
            currentTimeMillis in dawnStart until sunriseStart -> DiurnalSolarPhase.DAWN_TWILIGHT
            currentTimeMillis in sunriseStart until sunriseEnd -> DiurnalSolarPhase.GOLDEN_SUNRISE
            currentTimeMillis in sunriseEnd until noonStart -> DiurnalSolarPhase.MORNING_AZURE
            currentTimeMillis in noonStart until noonEnd -> DiurnalSolarPhase.SOLAR_NOON
            currentTimeMillis in noonEnd until sunsetStart -> DiurnalSolarPhase.AFTERNOON_WARMTH
            currentTimeMillis in sunsetStart until sunsetEnd -> DiurnalSolarPhase.GOLDEN_SUNSET
            currentTimeMillis in sunsetEnd until duskEnd -> DiurnalSolarPhase.DUSK_TWILIGHT
            else -> DiurnalSolarPhase.DEEP_NIGHT
        }
    }

    /**
     * Fallback based on hour-of-day if sunrise/sunset metadata is not yet downloaded.
     */
    private fun fallbackPhaseFromLocalClock(nowMillis: Long): DiurnalSolarPhase {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val fractionalHour = hour + (minute / 60.0)

        return when {
            fractionalHour in 5.25..6.2 -> DiurnalSolarPhase.DAWN_TWILIGHT
            fractionalHour in 6.2..7.5 -> DiurnalSolarPhase.GOLDEN_SUNRISE
            fractionalHour in 7.5..11.25 -> DiurnalSolarPhase.MORNING_AZURE
            fractionalHour in 11.25..13.75 -> DiurnalSolarPhase.SOLAR_NOON
            fractionalHour in 13.75..17.5 -> DiurnalSolarPhase.AFTERNOON_WARMTH
            fractionalHour in 17.5..19.2 -> DiurnalSolarPhase.GOLDEN_SUNSET
            fractionalHour in 19.2..20.75 -> DiurnalSolarPhase.DUSK_TWILIGHT
            else -> DiurnalSolarPhase.DEEP_NIGHT
        }
    }

    /**
     * Calculates the next diurnal phase transition and human-readable countdown summary.
     */
    fun calculateNextTransition(
        currentPhase: DiurnalSolarPhase,
        sunCycle: SunCycleInfo?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Pair<DiurnalSolarPhase, String> {
        val nextPhase = when (currentPhase) {
            DiurnalSolarPhase.DEEP_NIGHT -> DiurnalSolarPhase.DAWN_TWILIGHT
            DiurnalSolarPhase.DAWN_TWILIGHT -> DiurnalSolarPhase.GOLDEN_SUNRISE
            DiurnalSolarPhase.GOLDEN_SUNRISE -> DiurnalSolarPhase.MORNING_AZURE
            DiurnalSolarPhase.MORNING_AZURE -> DiurnalSolarPhase.SOLAR_NOON
            DiurnalSolarPhase.SOLAR_NOON -> DiurnalSolarPhase.AFTERNOON_WARMTH
            DiurnalSolarPhase.AFTERNOON_WARMTH -> DiurnalSolarPhase.GOLDEN_SUNSET
            DiurnalSolarPhase.GOLDEN_SUNSET -> DiurnalSolarPhase.DUSK_TWILIGHT
            DiurnalSolarPhase.DUSK_TWILIGHT -> DiurnalSolarPhase.DEEP_NIGHT
        }

        val summary = if (sunCycle != null && sunCycle.sunriseFormatted.isNotBlank() && sunCycle.sunsetFormatted.isNotBlank()) {
            when (currentPhase) {
                DiurnalSolarPhase.DEEP_NIGHT -> "Sunrise at ${sunCycle.sunriseFormatted}"
                DiurnalSolarPhase.DAWN_TWILIGHT -> "Sunrise in ~30m (${sunCycle.sunriseFormatted})"
                DiurnalSolarPhase.GOLDEN_SUNRISE -> "Morning Sky in ~40m"
                DiurnalSolarPhase.MORNING_AZURE -> "Solar Midday approaching"
                DiurnalSolarPhase.SOLAR_NOON -> "Sunset at ${sunCycle.sunsetFormatted}"
                DiurnalSolarPhase.AFTERNOON_WARMTH -> "Sunset in ~1h (${sunCycle.sunsetFormatted})"
                DiurnalSolarPhase.GOLDEN_SUNSET -> "Dusk Twilight in ~20m"
                DiurnalSolarPhase.DUSK_TWILIGHT -> "Deep Starlight Night approaching"
            }
        } else {
            "Next: ${nextPhase.shortLabel}"
        }

        return Pair(nextPhase, summary)
    }

    /**
     * Generates a richly tuned, harmonious color palette reflecting both the celestial diurnal phase
     * and atmospheric weather conditions (e.g. rain, storm, snow, fog, clear).
     */
    fun generatePalette(
        phase: DiurnalSolarPhase,
        condition: WeatherConditionType,
        isDay: Boolean = phase.isDaytime
    ): DiurnalPalette {
        // Base diurnal palettes
        return when (phase) {
            DiurnalSolarPhase.DEEP_NIGHT -> {
                when (condition) {
                    WeatherConditionType.THUNDERSTORM -> DiurnalPalette(
                        phase = phase,
                        primaryAccent = Color(0xFFC084FC),
                        secondaryAccent = Color(0xFF38BDF8),
                        tertiaryAccent = Color(0xFFF43F5E),
                        primaryGlow = Color(0x55C084FC),
                        gradientStops = listOf(
                            Color(0xFF090616),
                            Color(0xFF140C2C),
                            Color(0xFF221144),
                            Color(0xFF30155C)
                        ),
                        celestialGlow1 = Color(0xFF818CF8).copy(alpha = 0.28f),
                        celestialGlow2 = Color(0xFFC084FC).copy(alpha = 0.22f),
                        horizonGlow = Color(0xFF38BDF8).copy(alpha = 0.16f),
                        glassFillStart = Color(0x28FFFFFF),
                        glassFillEnd = Color(0x06FFFFFF),
                        glassBorderStart = Color(0x3CC084FC),
                        glassBorderEnd = Color(0x10FFFFFF),
                        cardHighlight = Color(0x33C084FC),
                        badgeBackground = Color(0x28C084FC),
                        chipBorder = Color(0x4DC084FC)
                    )
                    WeatherConditionType.SNOWY -> DiurnalPalette(
                        phase = phase,
                        primaryAccent = Color(0xFF93C5FD),
                        secondaryAccent = Color(0xFFBAE6FD),
                        tertiaryAccent = Color(0xFFE0E7FF),
                        primaryGlow = Color(0x4D93C5FD),
                        gradientStops = listOf(
                            Color(0xFF060B14),
                            Color(0xFF0D1728),
                            Color(0xFF15253D),
                            Color(0xFF1F3554)
                        ),
                        celestialGlow1 = Color(0xFFBAE6FD).copy(alpha = 0.25f),
                        celestialGlow2 = Color(0xFF60A5FA).copy(alpha = 0.20f),
                        horizonGlow = Color(0xFF93C5FD).copy(alpha = 0.15f),
                        glassFillStart = Color(0x25FFFFFF),
                        glassFillEnd = Color(0x05FFFFFF),
                        glassBorderStart = Color(0x3593C5FD),
                        glassBorderEnd = Color(0x0EFFFFFF),
                        cardHighlight = Color(0x2893C5FD),
                        badgeBackground = Color(0x2293C5FD),
                        chipBorder = Color(0x4093C5FD)
                    )
                    else -> DiurnalPalette(
                        phase = phase,
                        primaryAccent = Color(0xFF818CF8),
                        secondaryAccent = Color(0xFF38BDF8),
                        tertiaryAccent = Color(0xFFC7D2FE),
                        primaryGlow = Color(0x4D818CF8),
                        gradientStops = listOf(
                            Color(0xFF04060E),
                            Color(0xFF080D1D),
                            Color(0xFF101733),
                            Color(0xFF1B1C48)
                        ),
                        celestialGlow1 = Color(0xFF818CF8).copy(alpha = 0.26f),
                        celestialGlow2 = Color(0xFF4F46E5).copy(alpha = 0.22f),
                        horizonGlow = Color(0xFF38BDF8).copy(alpha = 0.14f),
                        glassFillStart = Color(0x20FFFFFF),
                        glassFillEnd = Color(0x05FFFFFF),
                        glassBorderStart = Color(0x30818CF8),
                        glassBorderEnd = Color(0x0DFFFFFF),
                        cardHighlight = Color(0x28818CF8),
                        badgeBackground = Color(0x22818CF8),
                        chipBorder = Color(0x3E818CF8)
                    )
                }
            }

            DiurnalSolarPhase.DAWN_TWILIGHT -> {
                DiurnalPalette(
                    phase = phase,
                    primaryAccent = Color(0xFFA5B4FC),
                    secondaryAccent = Color(0xFFF472B6),
                    tertiaryAccent = Color(0xFF38BDF8),
                    primaryGlow = Color(0x4DA5B4FC),
                    gradientStops = listOf(
                        Color(0xFF070B1A),
                        Color(0xFF121738),
                        Color(0xFF221E4E),
                        Color(0xFF38235E)
                    ),
                    celestialGlow1 = Color(0xFFF472B6).copy(alpha = 0.26f),
                    celestialGlow2 = Color(0xFFA5B4FC).copy(alpha = 0.24f),
                    horizonGlow = Color(0xFF818CF8).copy(alpha = 0.20f),
                    glassFillStart = Color(0x25FFFFFF),
                    glassFillEnd = Color(0x06FFFFFF),
                    glassBorderStart = Color(0x3FA5B4FC),
                    glassBorderEnd = Color(0x12FFFFFF),
                    cardHighlight = Color(0x30A5B4FC),
                    badgeBackground = Color(0x25A5B4FC),
                    chipBorder = Color(0x45A5B4FC)
                )
            }

            DiurnalSolarPhase.GOLDEN_SUNRISE -> {
                when (condition) {
                    WeatherConditionType.RAINY, WeatherConditionType.THUNDERSTORM -> DiurnalPalette(
                        phase = phase,
                        primaryAccent = Color(0xFFFFB020),
                        secondaryAccent = Color(0xFFFB7185),
                        tertiaryAccent = Color(0xFF38BDF8),
                        primaryGlow = Color(0x55FFB020),
                        gradientStops = listOf(
                            Color(0xFF140F20),
                            Color(0xFF2B1630),
                            Color(0xFF4C1D38),
                            Color(0xFF6B2636)
                        ),
                        celestialGlow1 = Color(0xFFFFB020).copy(alpha = 0.32f),
                        celestialGlow2 = Color(0xFFFB7185).copy(alpha = 0.25f),
                        horizonGlow = Color(0xFFFF7E33).copy(alpha = 0.24f),
                        glassFillStart = Color(0x2CFFFFFF),
                        glassFillEnd = Color(0x07FFFFFF),
                        glassBorderStart = Color(0x4DFFB020),
                        glassBorderEnd = Color(0x14FFFFFF),
                        cardHighlight = Color(0x35FFB020),
                        badgeBackground = Color(0x2AFFB020),
                        chipBorder = Color(0x50FFB020)
                    )
                    else -> DiurnalPalette(
                        phase = phase,
                        primaryAccent = Color(0xFFFFB020),
                        secondaryAccent = Color(0xFFFB7185),
                        tertiaryAccent = Color(0xFFFFD200),
                        primaryGlow = Color(0x55FFB020),
                        gradientStops = listOf(
                            Color(0xFF180F25),
                            Color(0xFF38183A),
                            Color(0xFF682240),
                            Color(0xFF91362A)
                        ),
                        celestialGlow1 = Color(0xFFFFB020).copy(alpha = 0.36f),
                        celestialGlow2 = Color(0xFFFB7185).copy(alpha = 0.28f),
                        horizonGlow = Color(0xFFFF6B2B).copy(alpha = 0.28f),
                        glassFillStart = Color(0x30FFFFFF),
                        glassFillEnd = Color(0x08FFFFFF),
                        glassBorderStart = Color(0x55FFB020),
                        glassBorderEnd = Color(0x15FFFFFF),
                        cardHighlight = Color(0x38FFB020),
                        badgeBackground = Color(0x2DFFB020),
                        chipBorder = Color(0x58FFB020)
                    )
                }
            }

            DiurnalSolarPhase.MORNING_AZURE -> {
                when (condition) {
                    WeatherConditionType.CLOUDY, WeatherConditionType.FOGGY -> DiurnalPalette(
                        phase = phase,
                        primaryAccent = Color(0xFF38BDF8),
                        secondaryAccent = Color(0xFF00F2FE),
                        tertiaryAccent = Color(0xFFCBD5E1),
                        primaryGlow = Color(0x4D38BDF8),
                        gradientStops = listOf(
                            Color(0xFF0C1B2A),
                            Color(0xFF162E45),
                            Color(0xFF204260),
                            Color(0xFF2C597D)
                        ),
                        celestialGlow1 = Color(0xFF38BDF8).copy(alpha = 0.24f),
                        celestialGlow2 = Color(0xFF00F2FE).copy(alpha = 0.18f),
                        horizonGlow = Color(0xFF60A5FA).copy(alpha = 0.15f),
                        glassFillStart = Color(0x26FFFFFF),
                        glassFillEnd = Color(0x06FFFFFF),
                        glassBorderStart = Color(0x3D38BDF8),
                        glassBorderEnd = Color(0x10FFFFFF),
                        cardHighlight = Color(0x2C38BDF8),
                        badgeBackground = Color(0x2238BDF8),
                        chipBorder = Color(0x4238BDF8)
                    )
                    else -> DiurnalPalette(
                        phase = phase,
                        primaryAccent = Color(0xFF00F2FE),
                        secondaryAccent = Color(0xFF38BDF8),
                        tertiaryAccent = Color(0xFFFFD200),
                        primaryGlow = Color(0x5000F2FE),
                        gradientStops = listOf(
                            Color(0xFF0A223E),
                            Color(0xFF103E64),
                            Color(0xFF185B84),
                            Color(0xFF22789E)
                        ),
                        celestialGlow1 = Color(0xFFFFD200).copy(alpha = 0.28f),
                        celestialGlow2 = Color(0xFF00F2FE).copy(alpha = 0.25f),
                        horizonGlow = Color(0xFF38BDF8).copy(alpha = 0.22f),
                        glassFillStart = Color(0x2BFFFFFF),
                        glassFillEnd = Color(0x07FFFFFF),
                        glassBorderStart = Color(0x4800F2FE),
                        glassBorderEnd = Color(0x12FFFFFF),
                        cardHighlight = Color(0x3200F2FE),
                        badgeBackground = Color(0x2500F2FE),
                        chipBorder = Color(0x4C00F2FE)
                    )
                }
            }

            DiurnalSolarPhase.SOLAR_NOON -> {
                DiurnalPalette(
                    phase = phase,
                    primaryAccent = Color(0xFFFFE600),
                    secondaryAccent = Color(0xFF00F2FE),
                    tertiaryAccent = Color(0xFF38BDF8),
                    primaryGlow = Color(0x55FFE600),
                    gradientStops = listOf(
                        Color(0xFF082747),
                        Color(0xFF0E4670),
                        Color(0xFF166396),
                        Color(0xFF1D83B8)
                    ),
                    celestialGlow1 = Color(0xFFFFE600).copy(alpha = 0.35f),
                    celestialGlow2 = Color(0xFF00F2FE).copy(alpha = 0.26f),
                    horizonGlow = Color(0xFF38BDF8).copy(alpha = 0.24f),
                    glassFillStart = Color(0x2EFFFFFF),
                    glassFillEnd = Color(0x07FFFFFF),
                    glassBorderStart = Color(0x4DFFE600),
                    glassBorderEnd = Color(0x14FFFFFF),
                    cardHighlight = Color(0x35FFE600),
                    badgeBackground = Color(0x28FFE600),
                    chipBorder = Color(0x50FFE600)
                )
            }

            DiurnalSolarPhase.AFTERNOON_WARMTH -> {
                DiurnalPalette(
                    phase = phase,
                    primaryAccent = Color(0xFFFBBF24),
                    secondaryAccent = Color(0xFF38BDF8),
                    tertiaryAccent = Color(0xFFFB923C),
                    primaryGlow = Color(0x4DFBBF24),
                    gradientStops = listOf(
                        Color(0xFF0C2036),
                        Color(0xFF153856),
                        Color(0xFF1F5170),
                        Color(0xFF2B6B8A)
                    ),
                    celestialGlow1 = Color(0xFFFBBF24).copy(alpha = 0.30f),
                    celestialGlow2 = Color(0xFF38BDF8).copy(alpha = 0.22f),
                    horizonGlow = Color(0xFFFB923C).copy(alpha = 0.20f),
                    glassFillStart = Color(0x28FFFFFF),
                    glassFillEnd = Color(0x06FFFFFF),
                    glassBorderStart = Color(0x42FBBF24),
                    glassBorderEnd = Color(0x10FFFFFF),
                    cardHighlight = Color(0x2EFBBF24),
                    badgeBackground = Color(0x22FBBF24),
                    chipBorder = Color(0x46FBBF24)
                )
            }

            DiurnalSolarPhase.GOLDEN_SUNSET -> {
                when (condition) {
                    WeatherConditionType.RAINY, WeatherConditionType.THUNDERSTORM -> DiurnalPalette(
                        phase = phase,
                        primaryAccent = Color(0xFFF43F5E),
                        secondaryAccent = Color(0xFFFB923C),
                        tertiaryAccent = Color(0xFFA855F7),
                        primaryGlow = Color(0x55F43F5E),
                        gradientStops = listOf(
                            Color(0xFF1C0826),
                            Color(0xFF3A0F38),
                            Color(0xFF5E1540),
                            Color(0xFF7A2035)
                        ),
                        celestialGlow1 = Color(0xFFFB923C).copy(alpha = 0.32f),
                        celestialGlow2 = Color(0xFFF43F5E).copy(alpha = 0.28f),
                        horizonGlow = Color(0xFFA855F7).copy(alpha = 0.24f),
                        glassFillStart = Color(0x2CFFFFFF),
                        glassFillEnd = Color(0x07FFFFFF),
                        glassBorderStart = Color(0x4DF43F5E),
                        glassBorderEnd = Color(0x14FFFFFF),
                        cardHighlight = Color(0x35F43F5E),
                        badgeBackground = Color(0x2AF43F5E),
                        chipBorder = Color(0x50F43F5E)
                    )
                    else -> DiurnalPalette(
                        phase = phase,
                        primaryAccent = Color(0xFFF43F5E),
                        secondaryAccent = Color(0xFFFB923C),
                        tertiaryAccent = Color(0xFFFFD200),
                        primaryGlow = Color(0x55F43F5E),
                        gradientStops = listOf(
                            Color(0xFF20092B),
                            Color(0xFF4A1244),
                            Color(0xFF7A1D49),
                            Color(0xFF9E342B)
                        ),
                        celestialGlow1 = Color(0xFFFB923C).copy(alpha = 0.38f),
                        celestialGlow2 = Color(0xFFF43F5E).copy(alpha = 0.32f),
                        horizonGlow = Color(0xFFFF7E33).copy(alpha = 0.32f),
                        glassFillStart = Color(0x30FFFFFF),
                        glassFillEnd = Color(0x08FFFFFF),
                        glassBorderStart = Color(0x58F43F5E),
                        glassBorderEnd = Color(0x16FFFFFF),
                        cardHighlight = Color(0x38F43F5E),
                        badgeBackground = Color(0x2EF43F5E),
                        chipBorder = Color(0x5AF43F5E)
                    )
                }
            }

            DiurnalSolarPhase.DUSK_TWILIGHT -> {
                DiurnalPalette(
                    phase = phase,
                    primaryAccent = Color(0xFFC084FC),
                    secondaryAccent = Color(0xFF818CF8),
                    tertiaryAccent = Color(0xFFE0E7FF),
                    primaryGlow = Color(0x4DC084FC),
                    gradientStops = listOf(
                        Color(0xFF090D1C),
                        Color(0xFF15193B),
                        Color(0xFF261D52),
                        Color(0xFF381F5E)
                    ),
                    celestialGlow1 = Color(0xFFC084FC).copy(alpha = 0.28f),
                    celestialGlow2 = Color(0xFF818CF8).copy(alpha = 0.24f),
                    horizonGlow = Color(0xFF6366F1).copy(alpha = 0.20f),
                    glassFillStart = Color(0x24FFFFFF),
                    glassFillEnd = Color(0x05FFFFFF),
                    glassBorderStart = Color(0x3EC084FC),
                    glassBorderEnd = Color(0x10FFFFFF),
                    cardHighlight = Color(0x2EC084FC),
                    badgeBackground = Color(0x24C084FC),
                    chipBorder = Color(0x44C084FC)
                )
            }
        }
    }
}
