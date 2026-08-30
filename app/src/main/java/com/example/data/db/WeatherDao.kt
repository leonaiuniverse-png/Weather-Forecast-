package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather_cache ORDER BY lastSyncTimestamp DESC LIMIT 1")
    fun getLatestWeatherFlow(): Flow<WeatherCacheEntity?>

    @Query("SELECT * FROM weather_cache WHERE cityKey = :cityKey LIMIT 1")
    fun getWeatherForCityFlow(cityKey: String): Flow<WeatherCacheEntity?>

    @Query("SELECT * FROM weather_cache WHERE cityKey = :cityKey LIMIT 1")
    suspend fun getWeatherForCity(cityKey: String): WeatherCacheEntity?

    @Query("SELECT * FROM weather_cache ORDER BY lastSyncTimestamp DESC LIMIT 1")
    suspend fun getLatestWeather(): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(entity: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE cityKey = :cityKey")
    suspend fun deleteWeatherForCity(cityKey: String)

    @Query("DELETE FROM weather_cache WHERE lastSyncTimestamp < :timestampThreshold")
    suspend fun deleteExpiredWeather(timestampThreshold: Long)

    @Query("DELETE FROM weather_cache")
    suspend fun clearAll()
}
