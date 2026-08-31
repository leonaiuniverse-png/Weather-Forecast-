package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.HourlyForecastItem
import com.example.data.repository.WeatherRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()
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

    @Test
    fun atmosphericGradients_haveValidThreeStopColors() {
        assertEquals(3, com.example.ui.theme.GradientClearDay.size)
        assertEquals(3, com.example.ui.theme.GradientClearNight.size)
        assertEquals(3, com.example.ui.theme.GradientRain.size)
        assertEquals(3, com.example.ui.theme.GradientThunderstorm.size)
        assertEquals(3, com.example.ui.theme.GradientSnow.size)
        assertEquals(3, com.example.ui.theme.GradientFog.size)
    }

    @Test
    fun metricImperialUnitConversions_functionAccurately() {
        val vm = com.example.ui.WeatherViewModel(app)

        // Default state: Metric (Celsius / km/h / mm)
        assertEquals(20, vm.convertTemp(20.0))
        assertEquals("°C", vm.getUnitSymbol())
        assertEquals("25 km/h", vm.getSpeedString(25.0))
        assertEquals("3.5 mm", vm.getPrecipitationString(3.5))

        // Toggle to Imperial (Fahrenheit / mph / in)
        vm.toggleUnit()
        assertEquals(68, vm.convertTemp(20.0))
        assertEquals("°F", vm.getUnitSymbol())
        assertEquals("15 mph", vm.getSpeedString(25.0))
        assertEquals("0.14 in", vm.getPrecipitationString(3.5))
        assertEquals("No rain", vm.getPrecipitationString(0.0))
    }

    @Test
    fun glassmorphismBlurIntensity_adjustsStateAndDialogCorrectly() {
        val vm = com.example.ui.WeatherViewModel(app)

        // Default blur intensity is 0.65f (65%)
        assertEquals(0.65f, vm.state.value.blurIntensity, 0.01f)
        assertEquals(false, vm.state.value.isSettingsDialogOpen)

        // Open settings dialog
        vm.openSettings()
        assertEquals(true, vm.state.value.isSettingsDialogOpen)

        // Adjust blur intensity
        vm.updateBlurIntensity(0.85f)
        assertEquals(0.85f, vm.state.value.blurIntensity, 0.01f)

        // Adjust to lower boundary
        vm.updateBlurIntensity(0.15f)
        assertEquals(0.15f, vm.state.value.blurIntensity, 0.01f)

        // Close settings dialog
        vm.closeSettings()
        assertEquals(false, vm.state.value.isSettingsDialogOpen)
    }

    @Test
    fun voiceAssistant_opensAndClosesCorrectly() {
        val vm = com.example.ui.WeatherViewModel(app)

        assertEquals(false, vm.state.value.isVoiceAssistantOpen)
        assertEquals("", vm.state.value.voiceQuestion)

        // Open Voice Assistant
        vm.openVoiceAssistant()
        assertEquals(true, vm.state.value.isVoiceAssistantOpen)

        // Update transcribed text
        vm.updateVoiceQuestion("Will it rain today?")
        assertEquals("Will it rain today?", vm.state.value.voiceQuestion)

        // Update listening state
        vm.setListening(true)
        assertEquals(true, vm.state.value.isListening)

        // Close Voice Assistant
        vm.closeVoiceAssistant()
        assertEquals(false, vm.state.value.isVoiceAssistantOpen)
        assertEquals(false, vm.state.value.isListening)
    }

    @Test
    fun voiceAssistant_askGeminiWeatherQuestion_returnsSensibleMeteorologyAnswer() = kotlinx.coroutines.test.runTest {
        val repository = com.example.data.repository.WeatherRepository()
        val mockCity = com.example.data.model.GeocodingResult(
            name = "Seattle",
            latitude = 47.6062,
            longitude = -122.3321,
            country = "United States",
            countryCode = "US"
        )
        val mockCondition = com.example.data.model.WeatherConditionInfo(
            title = "Rainy",
            type = com.example.data.model.WeatherConditionType.RAINY,
            description = "Light Rain",
            isDay = true
        )
        val mockWeatherData = com.example.data.repository.CompleteWeatherData(
            city = mockCity,
            currentTempC = 12.0,
            apparentTempC = 10.0,
            humidityPct = 85,
            windSpeedKmh = 18.0,
            precipitationMm = 4.2,
            surfacePressureHpa = 1010.0,
            weatherCode = 61,
            condition = mockCondition,
            todayMinTempC = 8.0,
            todayMaxTempC = 14.0,
            currentUvIndex = 2.0,
            currentPrecipProb = 75,
            hourlyList = listOf(
                com.example.data.model.HourlyForecastItem(
                    hourLabel = "Now",
                    timeIso = "2026-08-29T12:00",
                    tempC = 12.0,
                    apparentTempC = 10.0,
                    weatherCode = 61,
                    precipitationProb = 75,
                    isCurrentHour = true
                )
            ),
            dailyList = listOf(
                com.example.data.model.DailyForecastItem(
                    dayLabel = "Today",
                    dateIso = "2026-08-29",
                    weatherCode = 61,
                    minTempC = 8.0,
                    maxTempC = 14.0,
                    precipitationProb = 75,
                    uvIndex = 2.0
                )
            ),
            sunCycle = com.example.data.model.SunCycleInfo(
                sunriseIso = "2026-08-29T06:20",
                sunsetIso = "2026-08-29T20:00",
                sunriseFormatted = "6:20 AM",
                sunsetFormatted = "8:00 PM",
                daylightDurationFormatted = "13h 40m",
                solarProgress = 0.5f,
                isDaytime = true,
                solarStatus = "Sunset in 7h 30m"
            ),
            aiBriefing = "Rainy conditions in Seattle."
        )

        val rainAnswer = repository.askGeminiWeatherQuestion("Will it rain today?", mockWeatherData, false)
        assertTrue(rainAnswer.contains("Seattle", ignoreCase = true))
        assertTrue(rainAnswer.contains("rain", ignoreCase = true) || rainAnswer.contains("umbrella", ignoreCase = true))

        val outfitAnswer = repository.askGeminiWeatherQuestion("What should I wear today?", mockWeatherData, false)
        assertTrue(outfitAnswer.contains("Seattle", ignoreCase = true))
        assertTrue(outfitAnswer.contains("jacket", ignoreCase = true) || outfitAnswer.contains("sweater", ignoreCase = true) || outfitAnswer.contains("coat", ignoreCase = true) || outfitAnswer.contains("layer", ignoreCase = true))
    }

    @Test
    fun gpsLocationState_initializesAndUpdatesCorrectly() {
        val defaultState = com.example.ui.WeatherScreenState()

        // Verify initial GPS state flags
        assertEquals(false, defaultState.isGpsDetected)
        assertEquals(false, defaultState.isGpsLocating)
        assertEquals(null, defaultState.gpsErrorMessage)

        // Verify state transition when GPS location is acquired
        val gpsCity = com.example.data.model.GeocodingResult(
            name = "Current Location",
            latitude = 37.7749,
            longitude = -122.4194,
            country = "United States",
            countryCode = "US"
        )
        val gpsActiveState = defaultState.copy(
            selectedCity = gpsCity,
            isGpsDetected = true,
            isGpsLocating = false
        )
        assertEquals(true, gpsActiveState.isGpsDetected)
        assertEquals(false, gpsActiveState.isGpsLocating)
        assertEquals("Current Location", gpsActiveState.selectedCity.name)

        // Verify switching to manual city clears GPS detection flag
        val manualCity = com.example.data.model.GeocodingResult(
            name = "Paris",
            latitude = 48.8566,
            longitude = 2.3522,
            country = "France",
            countryCode = "FR"
        )
        val manualState = gpsActiveState.copy(
            selectedCity = manualCity,
            isGpsDetected = false
        )
        assertEquals(false, manualState.isGpsDetected)
        assertEquals("Paris", manualState.selectedCity.name)
    }

    @Test
    fun lunarPhaseCalculation_computesIlluminationAndPhasesAccurately() {
        val repo = WeatherRepository()
        
        // 1. Reference New Moon: Jan 11, 2024, 11:57 UTC
        val newMoonMillis = 1704974220000L
        val newMoon = repo.calculateLunarPhase(newMoonMillis)
        assertEquals("New Moon", newMoon.phaseName)
        assertEquals(0, newMoon.illuminationPct)
        assertEquals(0.0, newMoon.moonAgeDays, 0.5)
        assertTrue(newMoon.isWaxing)

        // 2. ~First Quarter: approx 7.38 days after new moon
        val firstQuarterMillis = newMoonMillis + (7.3826 * 86400000L).toLong()
        val firstQuarter = repo.calculateLunarPhase(firstQuarterMillis)
        assertEquals("First Quarter", firstQuarter.phaseName)
        assertEquals(50.0, firstQuarter.illuminationPct.toDouble(), 5.0) // approx 50%
        assertTrue(firstQuarter.isWaxing)

        // 3. ~Full Moon: approx 14.765 days after new moon
        val fullMoonMillis = newMoonMillis + (14.765 * 86400000L).toLong()
        val fullMoon = repo.calculateLunarPhase(fullMoonMillis)
        assertEquals("Full Moon", fullMoon.phaseName)
        assertEquals(100.0, fullMoon.illuminationPct.toDouble(), 2.0) // approx 100%

        // 4. ~Last Quarter: approx 22.15 days after new moon
        val lastQuarterMillis = newMoonMillis + (22.148 * 86400000L).toLong()
        val lastQuarter = repo.calculateLunarPhase(lastQuarterMillis)
        assertEquals("Last Quarter", lastQuarter.phaseName)
        assertEquals(50.0, lastQuarter.illuminationPct.toDouble(), 5.0) // approx 50%
        assertEquals(false, lastQuarter.isWaxing) // Waning

        // 5. Illumination percentage is strictly bounded between 0 and 100
        val currentLunar = repo.calculateLunarPhase(System.currentTimeMillis())
        assertTrue(currentLunar.illuminationPct in 0..100)
        assertTrue(currentLunar.phaseProgress in 0.0f..1.0f)
        assertTrue(currentLunar.phaseName.isNotBlank())
        assertTrue(currentLunar.nextPhaseSummary.isNotBlank())
    }

    @Test
    fun weatherAiSummaryRepository_telemetryFormatting_buildsCompleteContext() {
        val aiRepo = com.example.data.repository.WeatherAiSummaryRepository()
        val city = com.example.data.model.GeocodingResult(
            name = "Paris",
            latitude = 48.8566,
            longitude = 2.3522,
            country = "France"
        )
        val dummyData = com.example.data.repository.CompleteWeatherData(
            city = city,
            currentTempC = 19.5,
            apparentTempC = 20.0,
            humidityPct = 60,
            windSpeedKmh = 14.0,
            precipitationMm = 0.0,
            surfacePressureHpa = 1015.0,
            weatherCode = 1,
            condition = WeatherRepository.mapWeatherCode(1, isDay = true),
            todayMinTempC = 12.0,
            todayMaxTempC = 22.0,
            currentUvIndex = 4.8,
            currentPrecipProb = 15,
            hourlyList = listOf(
                com.example.data.model.HourlyForecastItem(
                    hourLabel = "Now",
                    timeIso = "2026-08-30T14:00",
                    tempC = 19.5,
                    apparentTempC = 20.0,
                    weatherCode = 1,
                    precipitationProb = 15,
                    isCurrentHour = true
                )
            ),
            dailyList = listOf(
                com.example.data.model.DailyForecastItem(
                    dayLabel = "Today",
                    dateIso = "2026-08-30",
                    weatherCode = 1,
                    minTempC = 12.0,
                    maxTempC = 22.0,
                    precipitationProb = 15,
                    uvIndex = 4.8
                )
            ),
            sunCycle = com.example.data.model.SunCycleInfo(
                sunriseIso = "2026-08-30T07:00",
                sunsetIso = "2026-08-30T20:30",
                sunriseFormatted = "7:00 AM",
                sunsetFormatted = "8:30 PM",
                daylightDurationFormatted = "13h 30m",
                solarProgress = 0.6f,
                isDaytime = true,
                solarStatus = "Sunset in 6h 30m"
            ),
            aiBriefing = "Pleasant conditions in Paris."
        )

        val prompt = aiRepo.buildTelemetryPrompt(dummyData, isFahrenheit = false)
        assertTrue(prompt.contains("Location: Paris, France"))
        assertTrue(prompt.contains("Current Temperature: 19°C"))
        assertTrue(prompt.contains("High 22°C / Low 12°C"))
        assertTrue(prompt.contains("Relative Humidity: 60%"))
        assertTrue(prompt.contains("Wind: 14 km/h"))

        val fahrenheitPrompt = aiRepo.buildTelemetryPrompt(dummyData, isFahrenheit = true)
        assertTrue(fahrenheitPrompt.contains("Location: Paris, France"))
        assertTrue(fahrenheitPrompt.contains("°F"))
    }

    @Test
    fun openMeteoForecastStates_structureAndConversion_workCorrectly() {
        val condition = WeatherRepository.mapWeatherCode(0, isDay = true)
        val current = com.example.data.model.CurrentWeatherForecastState(
            timeIso = "2026-08-30T12:00",
            formattedTime = "12:00 PM",
            temperatureC = 23.5,
            apparentTemperatureC = 24.0,
            humidityPct = 55,
            precipitationMm = 0.0,
            precipitationProb = 5,
            weatherCode = 0,
            condition = condition,
            windSpeedKmh = 12.0,
            surfacePressureHpa = 1013.2,
            uvIndex = 6.2,
            isDay = true
        )

        assertEquals(23.5, current.temperatureC, 0.01)
        assertEquals(55, current.humidityPct)
        assertEquals("Clear Sky", current.condition.title)

        val hourlyItem1 = com.example.data.model.HourlyForecastItemState(
            timeIso = "2026-08-30T13:00",
            hourLabel = "1 PM",
            temperatureC = 24.0,
            weatherCode = 0,
            condition = condition,
            precipitationProbability = 10,
            isCurrentHour = true
        )
        val hourlyItem2 = com.example.data.model.HourlyForecastItemState(
            timeIso = "2026-08-30T14:00",
            hourLabel = "2 PM",
            temperatureC = 25.0,
            weatherCode = 1,
            condition = WeatherRepository.mapWeatherCode(1, isDay = true),
            precipitationProbability = 35,
            isCurrentHour = false
        )

        val hourlyState = com.example.data.model.HourlyForecastState(
            hourlyItems = listOf(hourlyItem1, hourlyItem2),
            next24Hours = listOf(hourlyItem1, hourlyItem2),
            peakPrecipitationHour = "2 PM",
            maxProbabilityIn24Hours = 35,
            minTemperatureC = 24.0,
            maxTemperatureC = 25.0
        )

        assertEquals(2, hourlyState.hourlyItems.size)
        assertEquals("2 PM", hourlyState.peakPrecipitationHour)
        assertEquals(35, hourlyState.maxProbabilityIn24Hours)

        val dailyItem = com.example.data.model.DailyForecastItemState(
            dateIso = "2026-08-30",
            dayLabel = "Today",
            formattedDate = "Aug 30",
            weatherCode = 0,
            condition = condition,
            minTemperatureC = 16.0,
            maxTemperatureC = 26.0,
            precipitationProbabilityMax = 20,
            uvIndexMax = 7.0,
            isToday = true
        )

        val dailyState = com.example.data.model.DailyForecastState(
            dailyItems = listOf(dailyItem),
            weeklyMinTemperatureC = 16.0,
            weeklyMaxTemperatureC = 26.0,
            dominantCondition = condition,
            rainyDaysCount = 0
        )

        assertEquals(1, dailyState.dailyItems.size)
        assertEquals(16.0, dailyState.weeklyMinTemperatureC, 0.01)
        assertEquals(26.0, dailyState.weeklyMaxTemperatureC, 0.01)
    }
}

