package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    @Json(name = "results") val results: List<GeocodingResult>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "country") val country: String? = null,
    @Json(name = "admin1") val admin1: String? = null,
    @Json(name = "country_code") val countryCode: String? = null
)

@JsonClass(generateAdapter = true)
data class ForecastResponse(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "current") val current: CurrentWeatherDto? = null,
    @Json(name = "hourly") val hourly: HourlyWeatherDto? = null,
    @Json(name = "daily") val daily: DailyWeatherDto? = null,
    @Json(name = "alerts") val alerts: List<AlertDto>? = null
)

@JsonClass(generateAdapter = true)
data class AlertDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "event") val event: String? = null,
    @Json(name = "headline") val headline: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "severity") val severity: String? = null,
    @Json(name = "urgency") val urgency: String? = null,
    @Json(name = "effective") val effective: String? = null,
    @Json(name = "expires") val expires: String? = null,
    @Json(name = "instruction") val instruction: String? = null,
    @Json(name = "area") val area: String? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @Json(name = "time") val time: String? = null,
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Double? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
    @Json(name = "precipitation") val precipitation: Double? = null,
    @Json(name = "weather_code") val weatherCode: Int? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
    @Json(name = "surface_pressure") val surfacePressure: Double? = null
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
    @Json(name = "time") val time: List<String>? = null,
    @Json(name = "temperature_2m") val temperature2m: List<Double>? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int>? = null,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
    @Json(name = "time") val time: List<String>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int>? = null,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>? = null,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>? = null,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double>? = null,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>? = null,
    @Json(name = "sunrise") val sunrise: List<String>? = null,
    @Json(name = "sunset") val sunset: List<String>? = null
)

// UI Domain Models

/**
 * Represents the current weather forecast state processed from Open-Meteo for UI display.
 */
data class CurrentWeatherForecastState(
    val timeIso: String,
    val formattedTime: String,
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val humidityPct: Int,
    val precipitationMm: Double,
    val precipitationProb: Int = 0,
    val weatherCode: Int,
    val condition: WeatherConditionInfo,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int = 0,
    val surfacePressureHpa: Double,
    val uvIndex: Double = 0.0,
    val isDay: Boolean = true,
    val dewPointC: Double? = null,
    val cloudCoverPct: Int? = null,
    val visibilityMeters: Double? = null
)

/**
 * Represents an individual hourly forecast point formatted for Compose strips, charts, and pill items.
 */
data class HourlyForecastItemState(
    val timeIso: String,
    val hourLabel: String, // e.g., "Now", "2 PM", "15:00"
    val temperatureC: Double,
    val apparentTemperatureC: Double = temperatureC,
    val weatherCode: Int,
    val condition: WeatherConditionInfo,
    val precipitationProbability: Int, // 0 - 100%
    val precipitationMm: Double = 0.0,
    val windSpeedKmh: Double = 0.0,
    val humidityPct: Int = 0,
    val uvIndex: Double = 0.0,
    val isDay: Boolean = true,
    val isCurrentHour: Boolean = false
)

/**
 * Encapsulates the entire hourly forecast collection and metadata for the UI.
 */
data class HourlyForecastState(
    val hourlyItems: List<HourlyForecastItemState> = emptyList(),
    val next24Hours: List<HourlyForecastItemState> = emptyList(),
    val peakPrecipitationHour: String? = null,
    val maxProbabilityIn24Hours: Int = 0,
    val minTemperatureC: Double = 0.0,
    val maxTemperatureC: Double = 0.0
)

/**
 * Represents a single day's forecast state with thermal spans, solar metrics, and weather condition.
 */
data class DailyForecastItemState(
    val dateIso: String,
    val dayLabel: String, // e.g., "Today", "Tomorrow", "Mon", "Tue"
    val formattedDate: String, // e.g., "Aug 31", "31/08"
    val weatherCode: Int,
    val condition: WeatherConditionInfo,
    val minTemperatureC: Double,
    val maxTemperatureC: Double,
    val precipitationProbabilityMax: Int, // 0 - 100%
    val precipitationSumMm: Double = 0.0,
    val uvIndexMax: Double = 0.0,
    val sunriseIso: String? = null,
    val sunsetIso: String? = null,
    val sunriseFormatted: String? = null,
    val sunsetFormatted: String? = null,
    val daylightDurationMinutes: Int? = null,
    val isToday: Boolean = false
)

/**
 * Encapsulates the multi-day forecast sequence, weekly high/low temperature bounds, and summary trends.
 */
data class DailyForecastState(
    val dailyItems: List<DailyForecastItemState> = emptyList(),
    val weeklyMinTemperatureC: Double = 0.0,
    val weeklyMaxTemperatureC: Double = 0.0,
    val dominantCondition: WeatherConditionInfo? = null,
    val rainyDaysCount: Int = 0,
    val highestUvDay: DailyForecastItemState? = null
)

/**
 * Unified UI forecast state aggregating Current, Hourly, and Daily forecast dimensions from Open-Meteo.
 */
data class WeatherForecastState(
    val current: CurrentWeatherForecastState,
    val hourly: HourlyForecastState,
    val daily: DailyForecastState,
    val sunCycle: SunCycleInfo,
    val location: GeocodingResult,
    val activeAlerts: List<SevereWeatherAlert> = emptyList(),
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

data class LunarPhaseInfo(
    val phaseName: String,
    val illuminationPct: Int, // 0 to 100
    val phaseProgress: Float, // 0.0 (New Moon) to 0.5 (Full Moon) to 1.0
    val isWaxing: Boolean,
    val moonAgeDays: Double,
    val nextPhaseSummary: String
)

data class SunCycleInfo(
    val sunriseIso: String,
    val sunsetIso: String,
    val sunriseFormatted: String,
    val sunsetFormatted: String,
    val daylightDurationFormatted: String,
    val solarProgress: Float, // 0.0f (sunrise) to 1.0f (sunset), clamped or cycle-relative
    val isDaytime: Boolean,
    val solarStatus: String,
    val lunarPhase: LunarPhaseInfo? = null
)

data class HourlyForecastItem(
    val hourLabel: String,
    val timeIso: String,
    val tempC: Double,
    val apparentTempC: Double = tempC,
    val weatherCode: Int,
    val precipitationProb: Int,
    val isCurrentHour: Boolean = false
)

data class DailyForecastItem(
    val dayLabel: String,
    val dateIso: String,
    val weatherCode: Int,
    val minTempC: Double,
    val maxTempC: Double,
    val precipitationProb: Int,
    val uvIndex: Double
)

enum class WeatherConditionType {
    CLEAR_DAY,
    CLEAR_NIGHT,
    CLOUDY,
    RAINY,
    THUNDERSTORM,
    SNOWY,
    FOGGY
}

data class WeatherConditionInfo(
    val title: String,
    val type: WeatherConditionType,
    val description: String,
    val isDay: Boolean = true
)

enum class AlertSeverity {
    CRITICAL,
    WARNING,
    ADVISORY,
    WATCH
}

data class SevereWeatherAlert(
    val id: String,
    val event: String,
    val headline: String,
    val description: String,
    val severity: AlertSeverity = AlertSeverity.CRITICAL,
    val urgency: String = "Immediate",
    val effectiveTimeFormatted: String? = null,
    val expiresTimeFormatted: String? = null,
    val instruction: String? = null,
    val senderName: String = "National Meteorological Agency",
    val areaDesc: String? = null,
    val isCritical: Boolean = true
)
