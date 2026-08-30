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
import androidx.work.WorkManager
import com.example.data.model.GeocodingResult
import java.util.concurrent.TimeUnit

object AlertsWorkScheduler {
    private const val TAG = "AlertsWorkScheduler"
    private const val PERIODIC_WORK_NAME = "severe_weather_alerts_periodic_worker"
    private const val ONE_TIME_WORK_NAME = "severe_weather_alerts_immediate_check"

    /**
     * Enqueue a periodic background worker running every 15 minutes
     * to check Open-Meteo alerts for the active user location.
     */
    fun schedulePeriodicChecks(context: Context, city: GeocodingResult? = null) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val dataBuilder = Data.Builder()
            if (city != null) {
                dataBuilder.putDouble(SevereWeatherWorker.KEY_LATITUDE, city.latitude)
                dataBuilder.putDouble(SevereWeatherWorker.KEY_LONGITUDE, city.longitude)
                dataBuilder.putString(SevereWeatherWorker.KEY_CITY_NAME, city.name)
                dataBuilder.putString(SevereWeatherWorker.KEY_COUNTRY, city.country ?: "")
            }

            val periodicRequest = PeriodicWorkRequestBuilder<SevereWeatherWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // 5 min flex period
            )
                .setConstraints(constraints)
                .setInputData(dataBuilder.build())
                .addTag(SevereWeatherWorker.TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
            Log.d(TAG, "Periodic severe weather alerts worker successfully registered (15 min interval).")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic alerts worker: ${e.message}", e)
        }
    }

    /**
     * Trigger an immediate background check for a specific city location.
     */
    fun triggerImmediateCheck(context: Context, city: GeocodingResult) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putDouble(SevereWeatherWorker.KEY_LATITUDE, city.latitude)
                .putDouble(SevereWeatherWorker.KEY_LONGITUDE, city.longitude)
                .putString(SevereWeatherWorker.KEY_CITY_NAME, city.name)
                .putString(SevereWeatherWorker.KEY_COUNTRY, city.country ?: "")
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<SevereWeatherWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(SevereWeatherWorker.TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "Triggered immediate severe weather alert check for ${city.name}.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger immediate alert check: ${e.message}", e)
        }
    }
}
