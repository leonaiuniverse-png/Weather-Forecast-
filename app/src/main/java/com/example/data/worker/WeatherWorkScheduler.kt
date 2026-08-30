package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.data.model.GeocodingResult
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

object WeatherWorkScheduler {
    private const val TAG = "WeatherWorkScheduler"
    const val PERIODIC_WEATHER_WORK_NAME = "periodic_weather_sync_work"
    const val ONE_TIME_WEATHER_WORK_NAME = "immediate_weather_sync_work"

    /**
     * Enqueue a periodic background worker running every 15 minutes (with a 5-minute flex window)
     * to fetch weather data, update Room cache, and check severe weather alerts for the active location.
     */
    fun schedulePeriodicWeatherSync(context: Context, city: GeocodingResult) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putDouble(WeatherSyncWorker.KEY_LATITUDE, city.latitude)
                .putDouble(WeatherSyncWorker.KEY_LONGITUDE, city.longitude)
                .putString(WeatherSyncWorker.KEY_CITY_NAME, city.name)
                .putString(WeatherSyncWorker.KEY_COUNTRY, city.country ?: "")
                .putString(WeatherSyncWorker.KEY_ADMIN1, city.admin1 ?: "")
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // 5 min flex period
            )
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(WeatherSyncWorker.TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WEATHER_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
            Log.d(TAG, "Periodic weather sync scheduled every 15 minutes for ${city.name}.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic weather sync: ${e.message}", e)
        }
    }

    /**
     * Trigger an immediate on-demand background sync via WorkManager.
     */
    fun triggerImmediateSync(context: Context, city: GeocodingResult) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putDouble(WeatherSyncWorker.KEY_LATITUDE, city.latitude)
                .putDouble(WeatherSyncWorker.KEY_LONGITUDE, city.longitude)
                .putString(WeatherSyncWorker.KEY_CITY_NAME, city.name)
                .putString(WeatherSyncWorker.KEY_COUNTRY, city.country ?: "")
                .putString(WeatherSyncWorker.KEY_ADMIN1, city.admin1 ?: "")
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<WeatherSyncWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(WeatherSyncWorker.TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WEATHER_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "Triggered immediate background weather sync for ${city.name}.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger immediate background sync: ${e.message}", e)
        }
    }

    /**
     * Observe WorkManager execution status for periodic background sync.
     */
    fun observePeriodicWorkInfo(context: Context): Flow<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(PERIODIC_WEATHER_WORK_NAME)
    }

    fun cancelPeriodicSync(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WEATHER_WORK_NAME)
            Log.d(TAG, "Cancelled periodic weather sync.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel periodic sync: ${e.message}")
        }
    }
}
