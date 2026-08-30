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
    @Json(name = "daily") val daily: DailyWeatherDto? = null
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

data class SunCycleInfo(
    val sunriseIso: String,
    val sunsetIso: String,
    val sunriseFormatted: String,
    val sunsetFormatted: String,
    val daylightDurationFormatted: String,
    val solarProgress: Float, // 0.0f (sunrise) to 1.0f (sunset), clamped or cycle-relative
    val isDaytime: Boolean,
    val solarStatus: String
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
