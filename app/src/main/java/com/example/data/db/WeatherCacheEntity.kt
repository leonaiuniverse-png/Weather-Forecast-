package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DailyForecastItem
import com.example.data.model.GeocodingResult
import com.example.data.model.HourlyForecastItem
import com.example.data.model.LunarPhaseInfo
import com.example.data.model.SevereWeatherAlert
import com.example.data.model.SunCycleInfo
import com.example.data.model.WeatherConditionInfo
import com.example.data.model.WeatherConditionType
import com.example.data.repository.CompleteWeatherData
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val cityKey: String,
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val admin1: String?,
    val countryCode: String?,
    val currentTempC: Double,
    val apparentTempC: Double,
    val humidityPct: Int,
    val windSpeedKmh: Double,
    val precipitationMm: Double,
    val surfacePressureHpa: Double,
    val weatherCode: Int,
    val conditionTitle: String,
    val conditionType: String,
    val conditionDescription: String,
    val isDay: Boolean,
    val todayMinTempC: Double,
    val todayMaxTempC: Double,
    val currentUvIndex: Double,
    val currentPrecipProb: Int,
    val sunriseIso: String,
    val sunsetIso: String,
    val sunriseFormatted: String,
    val sunsetFormatted: String,
    val daylightDurationFormatted: String,
    val solarProgress: Float,
    val isDaytime: Boolean,
    val solarStatus: String,
    val moonPhaseName: String?,
    val moonIlluminationPct: Int?,
    val moonProgress: Float?,
    val moonIsWaxing: Boolean?,
    val moonAgeDays: Double?,
    val nextPhaseSummary: String?,
    val aiBriefing: String,
    val hourlyJson: String,
    val dailyJson: String,
    val alertsJson: String,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val syncSource: String = "BACKGROUND_WORKER"
) {
    companion object {
        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        }

        private val hourlyListAdapter by lazy {
            val type = Types.newParameterizedType(List::class.java, HourlyForecastItem::class.java)
            moshi.adapter<List<HourlyForecastItem>>(type)
        }

        private val dailyListAdapter by lazy {
            val type = Types.newParameterizedType(List::class.java, DailyForecastItem::class.java)
            moshi.adapter<List<DailyForecastItem>>(type)
        }

        private val alertsListAdapter by lazy {
            val type = Types.newParameterizedType(List::class.java, SevereWeatherAlert::class.java)
            moshi.adapter<List<SevereWeatherAlert>>(type)
        }

        fun createCityKey(city: GeocodingResult): String {
            val latRounded = (city.latitude * 1000).toInt()
            val lonRounded = (city.longitude * 1000).toInt()
            return "${city.name}_${latRounded}_${lonRounded}"
        }

        fun fromCompleteWeatherData(
            data: CompleteWeatherData,
            syncSource: String = "BACKGROUND_WORKER"
        ): WeatherCacheEntity {
            val city = data.city
            val sunCycle = data.sunCycle
            val lunar = sunCycle.lunarPhase

            val hourlyJson = try {
                hourlyListAdapter.toJson(data.hourlyList)
            } catch (e: Exception) {
                "[]"
            }

            val dailyJson = try {
                dailyListAdapter.toJson(data.dailyList)
            } catch (e: Exception) {
                "[]"
            }

            val alertsJson = try {
                alertsListAdapter.toJson(data.activeAlerts)
            } catch (e: Exception) {
                "[]"
            }

            return WeatherCacheEntity(
                cityKey = createCityKey(city),
                cityName = city.name,
                latitude = city.latitude,
                longitude = city.longitude,
                country = city.country,
                admin1 = city.admin1,
                countryCode = city.countryCode,
                currentTempC = data.currentTempC,
                apparentTempC = data.apparentTempC,
                humidityPct = data.humidityPct,
                windSpeedKmh = data.windSpeedKmh,
                precipitationMm = data.precipitationMm,
                surfacePressureHpa = data.surfacePressureHpa,
                weatherCode = data.weatherCode,
                conditionTitle = data.condition.title,
                conditionType = data.condition.type.name,
                conditionDescription = data.condition.description,
                isDay = data.condition.isDay,
                todayMinTempC = data.todayMinTempC,
                todayMaxTempC = data.todayMaxTempC,
                currentUvIndex = data.currentUvIndex,
                currentPrecipProb = data.currentPrecipProb,
                sunriseIso = sunCycle.sunriseIso,
                sunsetIso = sunCycle.sunsetIso,
                sunriseFormatted = sunCycle.sunriseFormatted,
                sunsetFormatted = sunCycle.sunsetFormatted,
                daylightDurationFormatted = sunCycle.daylightDurationFormatted,
                solarProgress = sunCycle.solarProgress,
                isDaytime = sunCycle.isDaytime,
                solarStatus = sunCycle.solarStatus,
                moonPhaseName = lunar?.phaseName,
                moonIlluminationPct = lunar?.illuminationPct,
                moonProgress = lunar?.phaseProgress,
                moonIsWaxing = lunar?.isWaxing,
                moonAgeDays = lunar?.moonAgeDays,
                nextPhaseSummary = lunar?.nextPhaseSummary,
                aiBriefing = data.aiBriefing,
                hourlyJson = hourlyJson,
                dailyJson = dailyJson,
                alertsJson = alertsJson,
                lastSyncTimestamp = System.currentTimeMillis(),
                syncSource = syncSource
            )
        }
    }

    fun toCompleteWeatherData(): CompleteWeatherData {
        val city = GeocodingResult(
            name = cityName,
            latitude = latitude,
            longitude = longitude,
            country = country,
            admin1 = admin1,
            countryCode = countryCode
        )

        val conditionTypeEnum = try {
            WeatherConditionType.valueOf(conditionType)
        } catch (e: Exception) {
            WeatherConditionType.CLEAR_DAY
        }

        val condition = WeatherConditionInfo(
            title = conditionTitle,
            type = conditionTypeEnum,
            description = conditionDescription,
            isDay = isDay
        )

        val lunarPhase = if (moonPhaseName != null) {
            LunarPhaseInfo(
                phaseName = moonPhaseName,
                illuminationPct = moonIlluminationPct ?: 50,
                phaseProgress = moonProgress ?: 0.5f,
                isWaxing = moonIsWaxing ?: true,
                moonAgeDays = moonAgeDays ?: 14.0,
                nextPhaseSummary = nextPhaseSummary ?: "Waxing Moon"
            )
        } else null

        val sunCycle = SunCycleInfo(
            sunriseIso = sunriseIso,
            sunsetIso = sunsetIso,
            sunriseFormatted = sunriseFormatted,
            sunsetFormatted = sunsetFormatted,
            daylightDurationFormatted = daylightDurationFormatted,
            solarProgress = solarProgress,
            isDaytime = isDaytime,
            solarStatus = solarStatus,
            lunarPhase = lunarPhase
        )

        val hourlyList: List<HourlyForecastItem> = try {
            hourlyListAdapter.fromJson(hourlyJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val dailyList: List<DailyForecastItem> = try {
            dailyListAdapter.fromJson(dailyJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val activeAlerts: List<SevereWeatherAlert> = try {
            alertsListAdapter.fromJson(alertsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return CompleteWeatherData(
            city = city,
            currentTempC = currentTempC,
            apparentTempC = apparentTempC,
            humidityPct = humidityPct,
            windSpeedKmh = windSpeedKmh,
            precipitationMm = precipitationMm,
            surfacePressureHpa = surfacePressureHpa,
            weatherCode = weatherCode,
            condition = condition,
            todayMinTempC = todayMinTempC,
            todayMaxTempC = todayMaxTempC,
            currentUvIndex = currentUvIndex,
            currentPrecipProb = currentPrecipProb,
            hourlyList = hourlyList,
            dailyList = dailyList,
            sunCycle = sunCycle,
            aiBriefing = aiBriefing,
            activeAlerts = activeAlerts,
            lastSyncTimestamp = lastSyncTimestamp,
            isFromCache = true
        )
    }
}
