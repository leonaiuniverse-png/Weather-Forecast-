package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.ApiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiGenerateRequest
import com.example.data.api.GeminiPart
import com.example.data.model.DailyForecastItem
import com.example.data.model.ForecastResponse
import com.example.data.model.GeocodingResult
import com.example.data.model.HourlyForecastItem
import com.example.data.model.WeatherConditionInfo
import com.example.data.model.WeatherConditionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    val aiBriefing: String
)

class WeatherRepository {
    private val openMeteo = ApiClient.openMeteoService
    private val gemini = ApiClient.geminiService

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
            aiBriefing = briefing
        )
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

        return com.example.data.model.SunCycleInfo(
            sunriseIso = rawSunrise,
            sunsetIso = rawSunset,
            sunriseFormatted = sunriseFormatted,
            sunsetFormatted = sunsetFormatted,
            daylightDurationFormatted = daylightFormatted,
            solarProgress = solarProgress,
            isDaytime = isDaytime,
            solarStatus = solarStatus
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
