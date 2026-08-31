package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.ApiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerateRequest
import com.example.data.api.GeminiPart
import com.example.data.alert.AlertStateManager
import com.example.data.db.WeatherCacheEntity
import com.example.data.db.WeatherDatabase
import com.example.data.model.AlertSeverity
import com.example.data.model.CurrentWeatherForecastState
import com.example.data.model.DailyForecastItem
import com.example.data.model.DailyForecastItemState
import com.example.data.model.DailyForecastState
import com.example.data.model.ForecastResponse
import com.example.data.model.GeocodingResult
import com.example.data.model.HourlyForecastItem
import com.example.data.model.HourlyForecastItemState
import com.example.data.model.HourlyForecastState
import com.example.data.model.SevereWeatherAlert
import com.example.data.model.WeatherConditionInfo
import com.example.data.model.WeatherConditionType
import com.example.data.model.WeatherForecastState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt

data class CompleteWeatherData(
    val city: GeocodingResult,
    val currentTempC: Double,
    val apparentTempC: Double,
    val humidityPct: Int,
    val windSpeedKmh: Double,
    val precipitationMm: Double,
    val surfacePressureHpa: Double,
    val weatherCode: Int,
    val condition: WeatherConditionInfo,
    val todayMinTempC: Double,
    val todayMaxTempC: Double,
    val currentUvIndex: Double,
    val currentPrecipProb: Int,
    val hourlyList: List<HourlyForecastItem>,
    val dailyList: List<DailyForecastItem>,
    val sunCycle: com.example.data.model.SunCycleInfo,
    val aiBriefing: String,
    val activeAlerts: List<SevereWeatherAlert> = emptyList(),
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val isFromCache: Boolean = false,
    val syncSource: String = "FOREGROUND"
) {
    /**
     * Converts the domain CompleteWeatherData into the structured UI forecast state.
     */
    fun toWeatherForecastState(): WeatherForecastState {
        val currentState = CurrentWeatherForecastState(
            timeIso = sunCycle.sunriseIso,
            formattedTime = "Now",
            temperatureC = currentTempC,
            apparentTemperatureC = apparentTempC,
            humidityPct = humidityPct,
            precipitationMm = precipitationMm,
            precipitationProb = currentPrecipProb,
            weatherCode = weatherCode,
            condition = condition,
            windSpeedKmh = windSpeedKmh,
            surfacePressureHpa = surfacePressureHpa,
            uvIndex = currentUvIndex,
            isDay = condition.isDay
        )

        val hourlyItemStates = hourlyList.map { item ->
            HourlyForecastItemState(
                timeIso = item.timeIso,
                hourLabel = item.hourLabel,
                temperatureC = item.tempC,
                apparentTemperatureC = item.apparentTempC,
                weatherCode = item.weatherCode,
                condition = WeatherRepository.mapWeatherCode(item.weatherCode, isDay = condition.isDay),
                precipitationProbability = item.precipitationProb,
                isCurrentHour = item.isCurrentHour
            )
        }

        val hourlyState = HourlyForecastState(
            hourlyItems = hourlyItemStates,
            next24Hours = hourlyItemStates.take(24),
            peakPrecipitationHour = hourlyItemStates.maxByOrNull { it.precipitationProbability }?.hourLabel,
            maxProbabilityIn24Hours = hourlyItemStates.take(24).maxOfOrNull { it.precipitationProbability } ?: 0,
            minTemperatureC = hourlyItemStates.minOfOrNull { it.temperatureC } ?: todayMinTempC,
            maxTemperatureC = hourlyItemStates.maxOfOrNull { it.temperatureC } ?: todayMaxTempC
        )

        val dailyItemStates = dailyList.mapIndexed { index, item ->
            DailyForecastItemState(
                dateIso = item.dateIso,
                dayLabel = item.dayLabel,
                formattedDate = item.dateIso,
                weatherCode = item.weatherCode,
                condition = WeatherRepository.mapWeatherCode(item.weatherCode, isDay = true),
                minTemperatureC = item.minTempC,
                maxTemperatureC = item.maxTempC,
                precipitationProbabilityMax = item.precipitationProb,
                uvIndexMax = item.uvIndex,
                isToday = index == 0
            )
        }

        val dailyState = DailyForecastState(
            dailyItems = dailyItemStates,
            weeklyMinTemperatureC = dailyItemStates.minOfOrNull { it.minTemperatureC } ?: todayMinTempC,
            weeklyMaxTemperatureC = dailyItemStates.maxOfOrNull { it.maxTemperatureC } ?: todayMaxTempC,
            dominantCondition = condition,
            rainyDaysCount = dailyItemStates.count { it.precipitationProbabilityMax >= 50 }
        )

        return WeatherForecastState(
            current = currentState,
            hourly = hourlyState,
            daily = dailyState,
            sunCycle = sunCycle,
            location = city,
            activeAlerts = activeAlerts,
            lastUpdatedTimestamp = lastSyncTimestamp
        )
    }
}

class WeatherRepository {
    private val openMeteo = ApiClient.openMeteoService
    private val gemini = ApiClient.geminiService

    suspend fun getCachedWeather(context: Context, city: GeocodingResult): CompleteWeatherData? = withContext(Dispatchers.IO) {
        try {
            val db = WeatherDatabase.getInstance(context)
            val cityKey = WeatherCacheEntity.createCityKey(city)
            val entity = db.weatherDao().getWeatherForCity(cityKey) ?: db.weatherDao().getLatestWeather()
            entity?.toCompleteWeatherData()
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Failed to load cached weather: ${e.message}")
            null
        }
    }

    suspend fun getLatestCachedWeather(context: Context): CompleteWeatherData? = withContext(Dispatchers.IO) {
        try {
            val db = WeatherDatabase.getInstance(context)
            val entity = db.weatherDao().getLatestWeather()
            entity?.toCompleteWeatherData()
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Failed to load latest cached weather: ${e.message}")
            null
        }
    }

    fun observeCachedWeather(context: Context, city: GeocodingResult): Flow<CompleteWeatherData?> {
        val db = WeatherDatabase.getInstance(context)
        val cityKey = WeatherCacheEntity.createCityKey(city)
        return db.weatherDao().getWeatherForCityFlow(cityKey).map { entity ->
            entity?.toCompleteWeatherData()
        }
    }

    suspend fun cacheWeatherData(context: Context, data: CompleteWeatherData, syncSource: String = "BACKGROUND_WORKER") = withContext(Dispatchers.IO) {
        try {
            val db = WeatherDatabase.getInstance(context)
            val entity = WeatherCacheEntity.fromCompleteWeatherData(data, syncSource)
            db.weatherDao().insertWeather(entity)
            Log.d("WeatherRepository", "Successfully persisted weather to Room cache for ${data.city.name} (Source: $syncSource).")
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Failed to cache weather data in Room: ${e.message}", e)
        }
    }

    suspend fun searchCities(query: String): List<GeocodingResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val response = openMeteo.searchCity(cityName = query.trim(), count = 6)
            response.results ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchWeather(city: GeocodingResult): CompleteWeatherData = withContext(Dispatchers.IO) {
        val forecast = openMeteo.getForecast(
            latitude = city.latitude,
            longitude = city.longitude
        )

        val current = forecast.current
        val currentTemp: Double = current?.temperature2m ?: 21.0
        val apparentTemp: Double = current?.apparentTemperature ?: currentTemp
        val humidity: Int = current?.relativeHumidity2m?.toInt() ?: 55
        val windSpeed: Double = current?.windSpeed10m ?: 12.0
        val precipitation: Double = current?.precipitation ?: 0.0
        val pressure: Double = current?.surfacePressure ?: 1013.25
        val weatherCode: Int = current?.weatherCode ?: 0

        // Parse Hourly
        val hourlyList = parseHourly(forecast)

        // Parse Daily
        val dailyList = parseDaily(forecast)

        // Parse Sun Cycle
        val sunCycle = parseSunCycle(forecast)

        val todayMin = dailyList.firstOrNull()?.minTempC ?: (currentTemp - 4)
        val todayMax = dailyList.firstOrNull()?.maxTempC ?: (currentTemp + 5)
        val currentUv = dailyList.firstOrNull()?.uvIndex ?: 4.5
        val currentPrecipProb: Int = hourlyList.firstOrNull()?.precipitationProb ?: (dailyList.firstOrNull()?.precipitationProb ?: 10)

        val condition = mapWeatherCode(weatherCode, isDay = sunCycle.isDaytime)

        // Parse and detect active severe weather alerts
        val activeAlerts = detectSevereAlerts(
            city = city,
            forecast = forecast,
            currentTempC = currentTemp,
            windSpeedKmh = windSpeed,
            precipMm = precipitation,
            weatherCode = weatherCode,
            maxTempC = todayMax,
            minTempC = todayMin,
            maxUv = currentUv
        )

        // Post the primary alert to shared AlertStateManager
        AlertStateManager.postAlert(activeAlerts.firstOrNull())

        // Generate Sky Intelligence briefing
        val briefing = generateSkyIntelligence(
            cityName = city.name,
            tempC = currentTemp,
            conditionTitle = condition.title,
            humidity = humidity,
            windSpeed = windSpeed,
            precipProb = currentPrecipProb,
            hourlyNext12h = hourlyList.take(12),
            todayMax = todayMax,
            uvIndex = currentUv
        )

        CompleteWeatherData(
            city = city,
            currentTempC = currentTemp,
            apparentTempC = apparentTemp,
            humidityPct = humidity,
            windSpeedKmh = windSpeed,
            precipitationMm = precipitation,
            surfacePressureHpa = pressure,
            weatherCode = weatherCode,
            condition = condition,
            todayMinTempC = todayMin,
            todayMaxTempC = todayMax,
            currentUvIndex = currentUv,
            currentPrecipProb = currentPrecipProb,
            hourlyList = hourlyList,
            dailyList = dailyList,
            sunCycle = sunCycle,
            aiBriefing = briefing,
            activeAlerts = activeAlerts
        ).also {
            // Synchronize top active alert to AlertStateManager
            if (activeAlerts.isNotEmpty()) {
                AlertStateManager.postAlert(activeAlerts.first())
            } else {
                AlertStateManager.clearAlert()
            }
        }
    }

    /**
     * Detects and constructs severe weather alerts from both Open-Meteo alerts DTOs
     * and real-time atmospheric threshold evaluations (convective storms, gales, floods, extreme temps).
     */
    fun detectSevereAlerts(
        city: GeocodingResult,
        forecast: ForecastResponse,
        currentTempC: Double,
        windSpeedKmh: Double,
        precipMm: Double,
        weatherCode: Int,
        maxTempC: Double,
        minTempC: Double,
        maxUv: Double
    ): List<SevereWeatherAlert> {
        val alerts = mutableListOf<SevereWeatherAlert>()

        // 1. Process external alerts returned from Open-Meteo API (if any)
        forecast.alerts?.forEachIndexed { index, dto ->
            val event = dto.event?.takeIf { it.isNotBlank() } ?: "Severe Weather Advisory"
            val headline = dto.headline?.takeIf { it.isNotBlank() } ?: "$event active for ${city.name}"
            val desc = dto.description?.takeIf { it.isNotBlank() } ?: "Hazardous weather conditions reported in this region."
            val severity = when (dto.severity?.lowercase(Locale.ROOT)) {
                "extreme", "severe", "critical" -> AlertSeverity.CRITICAL
                "warning", "moderate" -> AlertSeverity.WARNING
                "watch" -> AlertSeverity.WATCH
                else -> AlertSeverity.ADVISORY
            }
            alerts.add(
                SevereWeatherAlert(
                    id = dto.id ?: "om_alert_${city.name}_$index",
                    event = event,
                    headline = headline,
                    description = desc,
                    severity = severity,
                    urgency = dto.urgency ?: "Immediate",
                    effectiveTimeFormatted = dto.effective,
                    expiresTimeFormatted = dto.expires,
                    instruction = dto.instruction ?: "Follow directions from local emergency management authorities.",
                    senderName = "National Weather & Meteorological Service",
                    areaDesc = dto.area ?: "${city.name} Region",
                    isCritical = severity == AlertSeverity.CRITICAL || severity == AlertSeverity.WARNING
                )
            )
        }

        // 2. Convective Thunderstorm & Damaging Hail Hazard
        if (weatherCode in listOf(95, 96, 99)) {
            val isHail = weatherCode == 99 || weatherCode == 96
            alerts.add(
                SevereWeatherAlert(
                    id = "severe_tstorm_${city.name}",
                    event = if (isHail) "Severe Thunderstorm & Hail Warning" else "Severe Thunderstorm Warning",
                    headline = if (isHail) "Severe Convective Thunderstorm with Hail Risk in ${city.name}" else "Dangerous Thunderstorm Activity in ${city.name}",
                    description = "Atmospheric radar shows active convective cells with violent lightning discharges, localized downpours, and potential for damaging hail.",
                    severity = AlertSeverity.CRITICAL,
                    urgency = "Immediate",
                    instruction = "Seek shelter inside a sturdy building immediately. Stay clear of exterior windows and avoid using corded electrical devices.",
                    senderName = "Severe Storm Prediction Center",
                    areaDesc = "${city.name} and adjacent metro zones",
                    isCritical = true
                )
            )
        }

        // 3. Gale Force Wind Hazard
        if (windSpeedKmh >= 55.0) {
            alerts.add(
                SevereWeatherAlert(
                    id = "gale_wind_${city.name}",
                    event = "Gale Force High Wind Warning",
                    headline = "Sustained High Winds & Dangerous Gusts of ${windSpeedKmh.toInt()} km/h",
                    description = "High wind velocity can uproot trees, bring down overhead utility lines, and create severe driving hazards for high-profile vehicles.",
                    severity = AlertSeverity.CRITICAL,
                    urgency = "Immediate",
                    instruction = "Anchor outdoor loose items. Exercise extreme caution when driving near bridges and open terrain.",
                    senderName = "National Weather Hazard Center",
                    areaDesc = "${city.name} Valley & Ridge Corridors",
                    isCritical = true
                )
            )
        } else if (windSpeedKmh >= 42.0) {
            alerts.add(
                SevereWeatherAlert(
                    id = "wind_advisory_${city.name}",
                    event = "Wind Advisory",
                    headline = "Brisk Winds with Gusts Reaching ${windSpeedKmh.toInt()} km/h",
                    description = "Elevated wind speeds may cause difficulty driving and minor debris displacement.",
                    severity = AlertSeverity.ADVISORY,
                    urgency = "Expected",
                    instruction = "Secure patio furniture and monitor local wind forecasts.",
                    senderName = "National Weather Hazard Center",
                    areaDesc = "${city.name} Metro",
                    isCritical = false
                )
            )
        }

        // 4. Torrential Rain & Flash Flood Hazard
        if (precipMm >= 15.0 || weatherCode in listOf(65, 82)) {
            alerts.add(
                SevereWeatherAlert(
                    id = "flash_flood_${city.name}",
                    event = "Flash Flood & Torrential Rain Warning",
                    headline = "Severe Precipitation Rate Exceeding Runoff Capacity",
                    description = "Intense rain rate (${precipMm} mm/h) causing rapid water accumulation, urban street inundation, and low-lying creek swelling.",
                    severity = AlertSeverity.CRITICAL,
                    urgency = "Immediate",
                    instruction = "Never attempt to walk or drive through flood waters ('Turn Around, Don't Drown'). Move to higher elevation if threatened.",
                    senderName = "Hydrometeorological Center",
                    areaDesc = "${city.name} Low-Lying Basins",
                    isCritical = true
                )
            )
        }

        // 5. Heavy Snow & Blizzard Warning
        if (weatherCode in listOf(75, 86)) {
            alerts.add(
                SevereWeatherAlert(
                    id = "blizzard_${city.name}",
                    event = "Blizzard & Heavy Snowfall Warning",
                    headline = "Heavy Snow Accumulation & Substantially Reduced Visibility",
                    description = "Severe snowfall rates creating treacherous icy roadways and whiteout conditions.",
                    severity = AlertSeverity.CRITICAL,
                    urgency = "Immediate",
                    instruction = "Avoid unnecessary vehicular travel. Keep cold-weather emergency survival gear ready.",
                    senderName = "Winter Storm Warning Agency",
                    areaDesc = "${city.name} Regional Arteries",
                    isCritical = true
                )
            )
        }

        // 6. Excessive Heat Warning
        if (maxTempC >= 38.0 || currentTempC >= 38.0) {
            alerts.add(
                SevereWeatherAlert(
                    id = "extreme_heat_${city.name}",
                    event = "Excessive Heat Warning",
                    headline = "Dangerous Peak Heat Reaching ${maxTempC.toInt()}°C (${(maxTempC * 9/5 + 32).toInt()}°F)",
                    description = "Extreme heat index values increase the incidence of heat stroke and heat exhaustion under direct sun exposure.",
                    severity = AlertSeverity.WARNING,
                    urgency = "Immediate",
                    instruction = "Stay hydrated, limit strenuous outdoor activities during peak afternoon hours, and stay in air-conditioned environments.",
                    senderName = "Public Health & Climate Center",
                    areaDesc = "${city.name} Urban Area",
                    isCritical = false
                )
            )
        }

        // 7. Hard Freeze & Sub-Zero Cold
        if (minTempC <= -15.0 || currentTempC <= -15.0) {
            alerts.add(
                SevereWeatherAlert(
                    id = "hard_freeze_${city.name}",
                    event = "Hard Freeze & Extreme Cold Warning",
                    headline = "Sub-Zero Temperatures of ${minTempC.toInt()}°C (${(minTempC * 9/5 + 32).toInt()}°F)",
                    description = "Dangerous wind chills can cause frostbite on exposed skin in less than 30 minutes. Exposed pipes may freeze and rupture.",
                    severity = AlertSeverity.WARNING,
                    urgency = "Immediate",
                    instruction = "Cover exposed skin, bring companion animals indoors, and protect residential plumbing.",
                    senderName = "Public Health & Climate Center",
                    areaDesc = "${city.name} Region",
                    isCritical = false
                )
            )
        }

        return alerts
    }

    private fun parseSunCycle(forecast: ForecastResponse): com.example.data.model.SunCycleInfo {
        val sunriseList = forecast.daily?.sunrise
        val sunsetList = forecast.daily?.sunset

        val rawSunrise = sunriseList?.firstOrNull() ?: ""
        val rawSunset = sunsetList?.firstOrNull() ?: ""

        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val displayFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        val sunriseDate = try { if (rawSunrise.isNotBlank()) isoParser.parse(rawSunrise) else null } catch (e: Exception) { null }
        val sunsetDate = try { if (rawSunset.isNotBlank()) isoParser.parse(rawSunset) else null } catch (e: Exception) { null }

        val sunriseFormatted = sunriseDate?.let { displayFormat.format(it) } ?: "6:30 AM"
        val sunsetFormatted = sunsetDate?.let { displayFormat.format(it) } ?: "7:45 PM"

        val nowMillis = System.currentTimeMillis()
        val sunriseMillis = sunriseDate?.time ?: (nowMillis - 6 * 3600 * 1000)
        val sunsetMillis = sunsetDate?.time ?: (nowMillis + 6 * 3600 * 1000)

        val daylightDurationMillis = (sunsetMillis - sunriseMillis).coerceAtLeast(0)
        val totalHours = daylightDurationMillis / (1000 * 3600)
        val totalMinutes = (daylightDurationMillis / (1000 * 60)) % 60
        val daylightFormatted = "${totalHours}h ${totalMinutes}m"

        val isDaytime = nowMillis in sunriseMillis..sunsetMillis

        val solarProgress: Float = when {
            nowMillis < sunriseMillis -> 0f
            nowMillis > sunsetMillis -> 1f
            daylightDurationMillis > 0 -> ((nowMillis - sunriseMillis).toFloat() / daylightDurationMillis.toFloat()).coerceIn(0f, 1f)
            else -> 0.5f
        }

        val solarStatus = when {
            nowMillis < sunriseMillis -> {
                val mins = ((sunriseMillis - nowMillis) / (1000 * 60)).coerceAtLeast(1)
                val h = mins / 60
                val m = mins % 60
                if (h > 0) "Sunrise in ${h}h ${m}m" else "Sunrise in ${m}m"
            }
            isDaytime -> {
                val mins = ((sunsetMillis - nowMillis) / (1000 * 60)).coerceAtLeast(1)
                val h = mins / 60
                val m = mins % 60
                if (h > 0) "Sunset in ${h}h ${m}m" else "Sunset in ${m}m"
            }
            else -> {
                val nextSunrise = sunriseMillis + 24 * 3600 * 1000
                val mins = ((nextSunrise - nowMillis) / (1000 * 60)).coerceAtLeast(1)
                val h = mins / 60
                val m = mins % 60
                if (h > 0) "Sunrise in ${h}h ${m}m" else "Sunrise in ${m}m"
            }
        }

        val lunarPhase = calculateLunarPhase(nowMillis)

        return com.example.data.model.SunCycleInfo(
            sunriseIso = rawSunrise,
            sunsetIso = rawSunset,
            sunriseFormatted = sunriseFormatted,
            sunsetFormatted = sunsetFormatted,
            daylightDurationFormatted = daylightFormatted,
            solarProgress = solarProgress,
            isDaytime = isDaytime,
            solarStatus = solarStatus,
            lunarPhase = lunarPhase
        )
    }

    fun calculateLunarPhase(nowMillis: Long = System.currentTimeMillis()): com.example.data.model.LunarPhaseInfo {
        val synodicMonthDays = 29.530588853
        val synodicMonthMillis = synodicMonthDays * 86400000.0
        
        // Known reference New Moon epoch: Jan 11, 2024, 11:57 UTC
        val referenceNewMoonMillis = 1704974220000L
        
        val diffMillis = (nowMillis - referenceNewMoonMillis).toDouble()
        val cycleNormalizedMillis = ((diffMillis % synodicMonthMillis) + synodicMonthMillis) % synodicMonthMillis
        val moonAgeDays = cycleNormalizedMillis / 86400000.0
        val phaseProgress = (moonAgeDays / synodicMonthDays).toFloat().coerceIn(0f, 1f)
        
        // Illumination formula: (1 - cos(2 * PI * phaseProgress)) / 2
        val phaseAngleRad = 2.0 * Math.PI * phaseProgress
        val illuminationFraction = (1.0 - cos(phaseAngleRad)) / 2.0
        val illuminationPct = (illuminationFraction * 100.0).roundToInt().coerceIn(0, 100)
        
        val isWaxing = phaseProgress < 0.5f
        
        val phaseName = when {
            phaseProgress < 0.02f || phaseProgress >= 0.98f -> "New Moon"
            phaseProgress < 0.23f -> "Waxing Crescent"
            phaseProgress < 0.27f -> "First Quarter"
            phaseProgress < 0.48f -> "Waxing Gibbous"
            phaseProgress < 0.52f -> "Full Moon"
            phaseProgress < 0.73f -> "Waning Gibbous"
            phaseProgress < 0.77f -> "Last Quarter"
            else -> "Waning Crescent"
        }
        
        val nextMilestone = when {
            phaseProgress < 0.25f -> {
                val d = ((0.25 - phaseProgress) * synodicMonthDays).roundToInt().coerceAtLeast(1)
                "First Quarter in ${d}d"
            }
            phaseProgress < 0.50f -> {
                val d = ((0.50 - phaseProgress) * synodicMonthDays).roundToInt().coerceAtLeast(1)
                "Full Moon in ${d}d"
            }
            phaseProgress < 0.75f -> {
                val d = ((0.75 - phaseProgress) * synodicMonthDays).roundToInt().coerceAtLeast(1)
                "Last Quarter in ${d}d"
            }
            else -> {
                val d = ((1.0 - phaseProgress) * synodicMonthDays).roundToInt().coerceAtLeast(1)
                "New Moon in ${d}d"
            }
        }
        
        return com.example.data.model.LunarPhaseInfo(
            phaseName = phaseName,
            illuminationPct = illuminationPct,
            phaseProgress = phaseProgress,
            isWaxing = isWaxing,
            moonAgeDays = ((moonAgeDays * 10).roundToInt()) / 10.0,
            nextPhaseSummary = nextMilestone
        )
    }

    private fun parseHourly(forecast: ForecastResponse): List<HourlyForecastItem> {
        val hourly = forecast.hourly ?: return emptyList()
        val times = hourly.time ?: return emptyList()
        val temps = hourly.temperature2m ?: return emptyList()
        val apparentTemps = hourly.apparentTemperature
        val codes = hourly.weatherCode ?: return emptyList()
        val probs = hourly.precipitationProbability ?: return emptyList()

        val sdfParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val sdfHour = SimpleDateFormat("h a", Locale.getDefault())
        val now = Date()
        val currentHourInt = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val items = mutableListOf<HourlyForecastItem>()
        var foundCurrent = false

        for (i in times.indices) {
            val timeStr = times[i]
            val parsedDate = try { sdfParser.parse(timeStr) } catch (e: Exception) { null }
            val itemHour = parsedDate?.let {
                val cal = Calendar.getInstance().apply { time = it }
                cal.get(Calendar.HOUR_OF_DAY)
            } ?: i

            // Start showing from current hour or recent hours
            if (parsedDate != null && parsedDate.time < (now.time - 3600 * 1000) && times.size > 24) {
                continue
            }

            val isNow = !foundCurrent
            if (isNow) foundCurrent = true

            val label = if (isNow) "Now" else (parsedDate?.let { sdfHour.format(it) } ?: "${itemHour}:00")
            val baseTemp = temps.getOrElse(i) { 20.0 }
            val apparentTemp = apparentTemps?.getOrElse(i) { baseTemp } ?: baseTemp

            items.add(
                HourlyForecastItem(
                    hourLabel = label,
                    timeIso = timeStr,
                    tempC = baseTemp,
                    apparentTempC = apparentTemp,
                    weatherCode = codes.getOrElse(i) { 0 },
                    precipitationProb = probs.getOrElse(i) { 0 },
                    isCurrentHour = isNow
                )
            )

            if (items.size >= 24) break
        }

        return items
    }

    private fun parseDaily(forecast: ForecastResponse): List<DailyForecastItem> {
        val daily = forecast.daily ?: return emptyList()
        val times = daily.time ?: return emptyList()
        val codes = daily.weatherCode ?: return emptyList()
        val maxTemps = daily.temperature2mMax ?: return emptyList()
        val minTemps = daily.temperature2mMin ?: return emptyList()
        val uvs = daily.uvIndexMax ?: return emptyList()
        val probs = daily.precipitationProbabilityMax ?: return emptyList()

        val sdfParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDay = SimpleDateFormat("EEE", Locale.getDefault())

        val list = mutableListOf<DailyForecastItem>()

        for (i in times.indices) {
            val dateStr = times[i]
            val date = try { sdfParser.parse(dateStr) } catch (e: Exception) { null }
            val dayLabel = when (i) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> date?.let { sdfDay.format(it) } ?: "Day $i"
            }

            list.add(
                DailyForecastItem(
                    dayLabel = dayLabel,
                    dateIso = dateStr,
                    weatherCode = codes.getOrElse(i) { 0 },
                    minTempC = minTemps.getOrElse(i) { 15.0 },
                    maxTempC = maxTemps.getOrElse(i) { 25.0 },
                    precipitationProb = probs.getOrElse(i) { 0 },
                    uvIndex = uvs.getOrElse(i) { 5.0 }
                )
            )
            if (list.size >= 7) break
        }
        return list
    }

    private suspend fun generateSkyIntelligence(
        cityName: String,
        tempC: Double,
        conditionTitle: String,
        humidity: Int,
        windSpeed: Double,
        precipProb: Int,
        hourlyNext12h: List<HourlyForecastItem>,
        todayMax: Double,
        uvIndex: Double
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val weatherSummary = """
            {
              "city": "$cityName",
              "temperature_c": $tempC,
              "condition": "$conditionTitle",
              "humidity_percent": $humidity,
              "wind_speed_kmh": $windSpeed,
              "precipitation_probability": $precipProb,
              "today_max_temp_c": $todayMax,
              "uv_index": $uvIndex,
              "next_12_hours": ${hourlyNext12h.joinToString(prefix = "[", postfix = "]") { "{hour:'${it.hourLabel}', temp:${it.tempC}, pop:${it.precipitationProb}%}" }}
            }
        """.trimIndent()

        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = "Analyze this weather telemetry for $cityName: $weatherSummary"
                val request = GeminiGenerateRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                text = "You are an intelligent meteorologist. Given this weather JSON, generate a concise 2-sentence summary: 1st sentence on conditions and what to wear, 2nd sentence on the best outdoor window. Tone: friendly, modern, and practical. Do not include markdown asterisks or quotes."
                            )
                        )
                    )
                )
                val response = gemini.generateMeteorologyBriefing(apiKey = apiKey, request = request)
                val generated = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!generated.isNullOrBlank()) {
                    return generated
                }
            } catch (e: Exception) {
                // Fallback to intelligent local meteorological synthesizer
            }
        }

        // High-fidelity local rule-based meteorological briefing
        return generateLocalBriefing(cityName, tempC, conditionTitle, precipProb, windSpeed, uvIndex, hourlyNext12h)
    }

    suspend fun askGeminiWeatherQuestion(
        question: String,
        weather: CompleteWeatherData,
        isFahrenheit: Boolean
    ): String = withContext(Dispatchers.IO) {
        val trimmedQuery = question.trim()
        if (trimmedQuery.isBlank()) return@withContext "Please ask a weather question to get Gemini Sky Intelligence."

        val tempUnit = if (isFahrenheit) "°F" else "°C"
        val speedUnit = if (isFahrenheit) "mph" else "km/h"
        val displayTemp = if (isFahrenheit) ((weather.currentTempC * 9 / 5) + 32).toInt() else weather.currentTempC.toInt()
        val displayFeels = if (isFahrenheit) ((weather.apparentTempC * 9 / 5) + 32).toInt() else weather.apparentTempC.toInt()
        val displayHigh = if (isFahrenheit) ((weather.todayMaxTempC * 9 / 5) + 32).toInt() else weather.todayMaxTempC.toInt()
        val displayLow = if (isFahrenheit) ((weather.todayMinTempC * 9 / 5) + 32).toInt() else weather.todayMinTempC.toInt()
        val displayWind = if (isFahrenheit) (weather.windSpeedKmh * 0.621371).toInt() else weather.windSpeedKmh.toInt()

        val hourlySummary = weather.hourlyList.take(12).joinToString(", ") {
            val hTemp = if (isFahrenheit) ((it.tempC * 9 / 5) + 32).toInt() else it.tempC.toInt()
            "${it.hourLabel}: $hTemp$tempUnit (${it.precipitationProb}% rain)"
        }

        val dailySummary = weather.dailyList.take(5).joinToString(", ") {
            val dMax = if (isFahrenheit) ((it.maxTempC * 9 / 5) + 32).toInt() else it.maxTempC.toInt()
            val dMin = if (isFahrenheit) ((it.minTempC * 9 / 5) + 32).toInt() else it.minTempC.toInt()
            "${it.dayLabel}: high $dMax$tempUnit / low $dMin$tempUnit (${it.precipitationProb}% rain)"
        }

        val weatherTelemetry = """
            City: ${weather.city.name} (${weather.city.countryCode ?: weather.city.country})
            Current Condition: ${weather.condition.title} (${weather.condition.description})
            Current Temperature: $displayTemp$tempUnit (Feels like $displayFeels$tempUnit)
            Today High/Low: High $displayHigh$tempUnit / Low $displayLow$tempUnit
            Humidity: ${weather.humidityPct}%
            Wind Speed: $displayWind $speedUnit
            Precipitation Today: ${weather.precipitationMm} mm (Current rain chance: ${weather.currentPrecipProb}%)
            UV Index: ${String.format(Locale.US, "%.1f", weather.currentUvIndex)}
            Sunrise: ${weather.sunCycle.sunriseFormatted}, Sunset: ${weather.sunCycle.sunsetFormatted}
            Hourly Forecast (next 12h): $hourlySummary
            Daily Forecast: $dailySummary
        """.trimIndent()

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = "User Question: \"$trimmedQuery\"\n\nWeather Telemetry for ${weather.city.name}:\n$weatherTelemetry"
                val request = GeminiGenerateRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                text = "You are an intelligent, friendly AI weather assistant for a modern glassmorphic Android app. Answer the user's natural language question accurately using the provided weather telemetry for ${weather.city.name}. Provide a clear, natural, and concise answer (2 to 3 sentences maximum). Be specific with numbers, hours, or precipitation probabilities when relevant. Do not include markdown asterisks, bold characters, or quote marks in your response."
                            )
                        )
                    )
                )
                val response = gemini.generateMeteorologyBriefing(apiKey = apiKey, request = request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!text.isNullOrBlank()) {
                    return@withContext text
                }
            } catch (e: Exception) {
                // Fallback to local intelligent natural language answer processor
            }
        }

        // Local Rule-Based Natural Language Analyzer
        return@withContext processLocalWeatherQuestion(
            query = trimmedQuery,
            weather = weather,
            displayTemp = displayTemp,
            displayFeels = displayFeels,
            displayHigh = displayHigh,
            displayLow = displayLow,
            displayWind = displayWind,
            tempUnit = tempUnit,
            speedUnit = speedUnit
        )
    }

    private fun processLocalWeatherQuestion(
        query: String,
        weather: CompleteWeatherData,
        displayTemp: Int,
        displayFeels: Int,
        displayHigh: Int,
        displayLow: Int,
        displayWind: Int,
        tempUnit: String,
        speedUnit: String
    ): String {
        val q = query.lowercase(Locale.ROOT)
        val city = weather.city.name
        val rainChances = weather.hourlyList.take(12).filter { it.precipitationProb >= 30 }

        return when {
            q.contains("rain") || q.contains("umbrella") || q.contains("shower") || q.contains("precipitation") -> {
                if (weather.precipitationMm > 0.0 || weather.currentPrecipProb >= 50) {
                    "Yes, rain is expected in $city today with a ${weather.currentPrecipProb}% chance of precipitation. Be sure to carry an umbrella or rain shell."
                } else if (rainChances.isNotEmpty()) {
                    val times = rainChances.take(2).joinToString(" and ") { "${it.hourLabel} (${it.precipitationProb}%)" }
                    "There is a moderate chance of rain around $times in $city today. It's smart to pack a light umbrella."
                } else {
                    "No significant rain is in the forecast for $city today (only a ${weather.currentPrecipProb}% chance). You can leave your umbrella at home."
                }
            }
            q.contains("wear") || q.contains("jacket") || q.contains("clothes") || q.contains("outfit") || q.contains("coat") -> {
                when {
                    weather.currentTempC < 8 -> "It is cold in $city at $displayTemp$tempUnit. Bundle up in a heavy winter coat, warm layers, and a scarf."
                    weather.currentTempC in 8.0..16.0 -> "Temperatures are brisk around $displayTemp$tempUnit in $city. A cozy fleece jacket or layered sweater will keep you warm."
                    weather.currentTempC in 16.1..23.0 -> "Mild and pleasant conditions at $displayTemp$tempUnit in $city. A light cardigan, windbreaker, or long sleeve shirt is ideal."
                    else -> "Warm and summery at $displayTemp$tempUnit in $city. Lightweight breathable cotton, shorts, and sunglasses are recommended."
                }
            }
            q.contains("wind") || q.contains("breeze") || q.contains("gust") -> {
                if (weather.windSpeedKmh > 25) {
                    "It is fairly windy in $city with winds blowing at $displayWind $speedUnit. Hold on to your hats and secure lightweight outdoor items."
                } else {
                    "Wind conditions in $city are gentle at $displayWind $speedUnit with only calm breezes."
                }
            }
            q.contains("run") || q.contains("walk") || q.contains("outdoor") || q.contains("exercise") || q.contains("jog") -> {
                val bestHour = weather.hourlyList.take(12).minByOrNull { it.precipitationProb }?.hourLabel ?: "late afternoon"
                "The best time for outdoor activities in $city is around $bestHour when precipitation risk is lowest and conditions are steady."
            }
            q.contains("uv") || q.contains("sun") || q.contains("sunscreen") -> {
                if (weather.currentUvIndex >= 6.0) {
                    "The UV index in $city is elevated at ${String.format(Locale.US, "%.1f", weather.currentUvIndex)}. Make sure to apply SPF 30+ sunscreen and wear sunglasses if staying outside."
                } else {
                    "The UV index in $city is moderate at ${String.format(Locale.US, "%.1f", weather.currentUvIndex)}, requiring standard sun protection during peak daylight hours."
                }
            }
            q.contains("hot") || q.contains("temp") || q.contains("warm") || q.contains("cold") || q.contains("freeze") -> {
                "In $city, the current temperature is $displayTemp$tempUnit (feels like $displayFeels$tempUnit), with an expected high of $displayHigh$tempUnit and a low of $displayLow$tempUnit."
            }
            q.contains("tomorrow") -> {
                val tomorrow = weather.dailyList.getOrNull(1)
                if (tomorrow != null) {
                    val tMax = if (tempUnit == "°F") ((tomorrow.maxTempC * 9 / 5) + 32).toInt() else tomorrow.maxTempC.toInt()
                    val tMin = if (tempUnit == "°F") ((tomorrow.minTempC * 9 / 5) + 32).toInt() else tomorrow.minTempC.toInt()
                    "Tomorrow in $city will see a high of $tMax$tempUnit and a low of $tMin$tempUnit with a ${tomorrow.precipitationProb}% chance of rain."
                } else {
                    "Tomorrow's forecast in $city shows steady seasonal conditions with mild breezes."
                }
            }
            else -> {
                "In $city, it is currently $displayTemp$tempUnit and ${weather.condition.title.lowercase(Locale.ROOT)}. Today will reach a high of $displayHigh$tempUnit with a ${weather.currentPrecipProb}% chance of precipitation."
            }
        }
    }

    private fun generateLocalBriefing(
        cityName: String,
        tempC: Double,
        condition: String,
        precipProb: Int,
        windSpeed: Double,
        uvIndex: Double,
        hourly: List<HourlyForecastItem>
    ): String {
        val outfitAdvice = when {
            tempC < 5 -> "Frigid temperatures call for a heavy insulated coat, warm scarf, and gloves."
            tempC in 5.0..14.9 -> "Cooler ambient air makes a windbreaker or cozy layered knitwear ideal."
            tempC in 15.0..22.9 -> "Mild, comfortable conditions call for a light breathable jacket or long sleeves."
            tempC in 23.0..29.9 -> "Warm and pleasant vibes—cotton tees and sunglasses will keep you comfortable."
            else -> "High heat today; stay hydrated and opt for airy, lightweight fabrics."
        }

        val rainNote = if (precipProb > 45) {
            "Keep an umbrella handy as showers are likely developing."
        } else if (windSpeed > 30) {
            "Brisk wind gusts of ${windSpeed.toInt()} km/h may feel chilly."
        } else if (uvIndex >= 6) {
            "UV peak is elevated at ${String.format(Locale.US, "%.1f", uvIndex)}; apply SPF 30+."
        } else {
            "Clear and stable atmosphere expected."
        }

        // Find lowest rain probability window in hourly
        val bestWindow = hourly.filter { it.precipitationProb <= 20 }.take(4).map { it.hourLabel }
        val timingAdvice = if (bestWindow.isNotEmpty()) {
            "Your best outdoor window is around ${bestWindow.first()} for open-air strolls or jogging."
        } else {
            "Late afternoon offers the calmest stretch for daily errands."
        }

        return "$condition with ${tempC.toInt()}°C in $cityName—$outfitAdvice $timingAdvice"
    }

    private fun isDayTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour in 6..19
    }

    companion object {
        fun mapWeatherCode(code: Int, isDay: Boolean = true): WeatherConditionInfo {
            return when (code) {
                0 -> WeatherConditionInfo(
                    title = if (isDay) "Clear Sky" else "Clear Night",
                    type = if (isDay) WeatherConditionType.CLEAR_DAY else WeatherConditionType.CLEAR_NIGHT,
                    description = if (isDay) "Brilliant sunshine with unobstructed skies" else "Crisp starlit sky",
                    isDay = isDay
                )
                1 -> WeatherConditionInfo(
                    title = "Mainly Clear",
                    type = if (isDay) WeatherConditionType.CLEAR_DAY else WeatherConditionType.CLEAR_NIGHT,
                    description = "Mostly sunny with subtle high-altitude wisps",
                    isDay = isDay
                )
                2 -> WeatherConditionInfo(
                    title = "Partly Cloudy",
                    type = WeatherConditionType.CLOUDY,
                    description = "Scattered cumulus clouds with sun breaks",
                    isDay = isDay
                )
                3 -> WeatherConditionInfo(
                    title = "Overcast",
                    type = WeatherConditionType.CLOUDY,
                    description = "Uniform cloud blanket across the horizon",
                    isDay = isDay
                )
                45, 48 -> WeatherConditionInfo(
                    title = "Dense Fog",
                    type = WeatherConditionType.FOGGY,
                    description = "Reduced horizontal visibility in cool mist",
                    isDay = isDay
                )
                51, 53, 55 -> WeatherConditionInfo(
                    title = "Light Drizzle",
                    type = WeatherConditionType.RAINY,
                    description = "Gentle moisture droplets falling periodically",
                    isDay = isDay
                )
                56, 57 -> WeatherConditionInfo(
                    title = "Freezing Drizzle",
                    type = WeatherConditionType.RAINY,
                    description = "Chilly drizzle with surface glaze risk",
                    isDay = isDay
                )
                61 -> WeatherConditionInfo(
                    title = "Slight Rain",
                    type = WeatherConditionType.RAINY,
                    description = "Intermittent gentle rainfall",
                    isDay = isDay
                )
                63 -> WeatherConditionInfo(
                    title = "Moderate Rain",
                    type = WeatherConditionType.RAINY,
                    description = "Steady rhythmic precipitation across the city",
                    isDay = isDay
                )
                65 -> WeatherConditionInfo(
                    title = "Heavy Rain",
                    type = WeatherConditionType.RAINY,
                    description = "Substantial downpour with water accumulation",
                    isDay = isDay
                )
                66, 67 -> WeatherConditionInfo(
                    title = "Freezing Rain",
                    type = WeatherConditionType.RAINY,
                    description = "Icy rain creating slick outdoor conditions",
                    isDay = isDay
                )
                71, 73, 75, 77 -> WeatherConditionInfo(
                    title = "Snowfall",
                    type = WeatherConditionType.SNOWY,
                    description = "Crisp white snowflakes settling calmly",
                    isDay = isDay
                )
                80, 81, 82 -> WeatherConditionInfo(
                    title = "Scattered Showers",
                    type = WeatherConditionType.RAINY,
                    description = "Passing convective rain showers",
                    isDay = isDay
                )
                85, 86 -> WeatherConditionInfo(
                    title = "Snow Showers",
                    type = WeatherConditionType.SNOWY,
                    description = "Occasional bursts of drifting flurries",
                    isDay = isDay
                )
                95 -> WeatherConditionInfo(
                    title = "Thunderstorm",
                    type = WeatherConditionType.THUNDERSTORM,
                    description = "Electric lightning strikes with rumbling thunder",
                    isDay = isDay
                )
                96, 99 -> WeatherConditionInfo(
                    title = "Severe Thunderstorm",
                    type = WeatherConditionType.THUNDERSTORM,
                    description = "Intense storm cell with potential hail",
                    isDay = isDay
                )
                else -> WeatherConditionInfo(
                    title = "Variable Weather",
                    type = WeatherConditionType.CLOUDY,
                    description = "Typical regional atmospheric conditions",
                    isDay = isDay
                )
            }
        }

        val POPULAR_CITIES = listOf(
            GeocodingResult(name = "San Francisco", latitude = 37.7749, longitude = -122.4194, country = "United States", admin1 = "California", countryCode = "US"),
            GeocodingResult(name = "New York", latitude = 40.7128, longitude = -74.0060, country = "United States", admin1 = "New York", countryCode = "US"),
            GeocodingResult(name = "Tokyo", latitude = 35.6762, longitude = 139.6503, country = "Japan", admin1 = "Tokyo", countryCode = "JP"),
            GeocodingResult(name = "London", latitude = 51.5074, longitude = -0.1278, country = "United Kingdom", admin1 = "England", countryCode = "GB"),
            GeocodingResult(name = "Paris", latitude = 48.8566, longitude = 2.3522, country = "France", admin1 = "Île-de-France", countryCode = "FR"),
            GeocodingResult(name = "Sydney", latitude = -33.8688, longitude = 151.2093, country = "Australia", admin1 = "New South Wales", countryCode = "AU"),
            GeocodingResult(name = "Singapore", latitude = 1.3521, longitude = 103.8198, country = "Singapore", admin1 = null, countryCode = "SG"),
            GeocodingResult(name = "Dubai", latitude = 25.2048, longitude = 55.2708, country = "United Arab Emirates", admin1 = "Dubai", countryCode = "AE"),
            GeocodingResult(name = "Reykjavik", latitude = 64.1466, longitude = -21.9426, country = "Iceland", admin1 = "Capital Region", countryCode = "IS")
        )
    }
}
