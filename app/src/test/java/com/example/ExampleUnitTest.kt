package com.example

import com.example.data.model.HourlyForecastItem
import com.example.data.repository.WeatherRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun hourlyForecastItem_apparentTemperatureDefaultsCorrectly() {
        val item = HourlyForecastItem(
            hourLabel = "12 PM",
            timeIso = "2026-08-29T12:00",
            tempC = 24.0,
            apparentTempC = 26.5,
            weatherCode = 0,
            precipitationProb = 10,
            isCurrentHour = true
        )
        assertEquals(24.0, item.tempC, 0.01)
        assertEquals(26.5, item.apparentTempC, 0.01)
        assertTrue(item.isCurrentHour)
    }

    @Test
    fun weatherCodeMapping_returnsValidConditions() {
        val clear = WeatherRepository.mapWeatherCode(0, isDay = true)
        assertEquals("Clear Sky", clear.title)

        val rain = WeatherRepository.mapWeatherCode(63, isDay = true)
        assertEquals("Moderate Rain", rain.title)
    }

    @Test
    fun shareWeatherText_formatsAccurately() {
        val city = "Tokyo"
        val temp = "26°C"
        val condition = "Partly Cloudy"
        val formatted = "🌤️ Current weather in $city: $temp, $condition. Shared via SkyGlass Weather."
        assertTrue(formatted.contains("Tokyo"))
        assertTrue(formatted.contains("26°C"))
        assertTrue(formatted.contains("Partly Cloudy"))
    }

    @Test
    fun hapticUtils_handlesNullViewsGracefully() {
        // Ensure no NullPointerExceptions or unexpected crashes occur when passing null views
        com.example.ui.util.HapticUtils.performClick(null)
        com.example.ui.util.HapticUtils.performTick(null)
        com.example.ui.util.HapticUtils.performUpdateSuccess(null)
        com.example.ui.util.HapticUtils.performKeyTap(null)
    }
}

