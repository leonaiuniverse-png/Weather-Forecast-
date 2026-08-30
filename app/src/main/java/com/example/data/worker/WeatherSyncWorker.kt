package com.example.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.data.alert.AlertStateManager
import com.example.data.model.GeocodingResult
import com.example.data.model.SevereWeatherAlert
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background WorkManager Worker responsible for periodically fetching
 * atmospheric telemetry, forecasts, lunar & solar cycles, AI briefings, and
 * severe weather alerts.
 *
 * Persists the latest weather directly to Room Database so the dashboard is
 * instantly fresh whenever opened, even without launching the app beforehand.
 */
class WeatherSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "WeatherSyncWorker"
        const val KEY_LATITUDE = "key_latitude"
        const val KEY_LONGITUDE = "key_longitude"
        const val KEY_CITY_NAME = "key_city_name"
        const val KEY_COUNTRY = "key_country"
        const val KEY_ADMIN1 = "key_admin1"

        const val CHANNEL_ID_ALERTS = "channel_severe_weather_alerts"
        const val CHANNEL_ID_SYNC = "channel_weather_sync_updates"
        const val NOTIFICATION_ID_ALERT = 9001
        const val NOTIFICATION_ID_SYNC = 9002
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Executing background WeatherSyncWorker via WorkManager...")
            val lat = inputData.getDouble(KEY_LATITUDE, 37.7749)
            val lon = inputData.getDouble(KEY_LONGITUDE, -122.4194)
            val cityName = inputData.getString(KEY_CITY_NAME) ?: "San Francisco"
            val country = inputData.getString(KEY_COUNTRY) ?: "United States"
            val admin1 = inputData.getString(KEY_ADMIN1)

            val city = GeocodingResult(
                name = cityName,
                latitude = lat,
                longitude = lon,
                country = country,
                admin1 = admin1
            )

            val repository = WeatherRepository()

            // 1. Fetch live meteorological data from Open-Meteo & Gemini
            val liveData = repository.fetchWeather(city)

            // 2. Persist fresh telemetry to Room Database for instant UI hydration
            val syncedData = liveData.copy(
                lastSyncTimestamp = System.currentTimeMillis(),
                isFromCache = false,
                syncSource = "BACKGROUND_WORKER"
            )
            repository.cacheWeatherData(context, syncedData, syncSource = "BACKGROUND_WORKER")

            // 3. Update active alerts
            val activeAlerts = syncedData.activeAlerts
            if (activeAlerts.isNotEmpty()) {
                val primaryAlert = activeAlerts.first()
                Log.w(TAG, "Severe alert detected in background: ${primaryAlert.headline}")
                AlertStateManager.postAlert(primaryAlert)
                showSevereAlertNotification(context, primaryAlert)
            } else {
                AlertStateManager.clearAlert()
            }

            Log.i(TAG, "Background weather sync completed successfully for ${city.name}: ${syncedData.currentTempC}°C, ${syncedData.condition.title}")

            Result.success(
                workDataOf(
                    "sync_success" to true,
                    "city" to city.name,
                    "temperature_c" to syncedData.currentTempC,
                    "condition" to syncedData.condition.title,
                    "timestamp" to syncedData.lastSyncTimestamp,
                    "alerts_count" to activeAlerts.size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Background WeatherSyncWorker encountered error: ${e.message}", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun showSevereAlertNotification(context: Context, alert: SevereWeatherAlert) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID_ALERTS,
                    "Severe Weather & Emergency Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical real-time alerts for thunderstorms, flash floods, gale winds, and severe atmospheric hazards."
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_SEVERE_ALERT_ID", alert.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ ${alert.event}")
                .setContentText(alert.headline)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${alert.headline}\n\n${alert.description}\n\nSafety: ${alert.instruction ?: "Take necessary weather precautions."}")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ALERT, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted, skipping alert notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display severe alert notification: ${e.message}")
        }
    }
}
