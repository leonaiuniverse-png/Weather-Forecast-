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
import com.example.R
import com.example.data.alert.AlertStateManager
import com.example.data.api.ApiClient
import com.example.data.model.GeocodingResult
import com.example.data.model.SevereWeatherAlert
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SevereWeatherWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "SevereWeatherWorker"
        const val KEY_LATITUDE = "key_latitude"
        const val KEY_LONGITUDE = "key_longitude"
        const val KEY_CITY_NAME = "key_city_name"
        const val KEY_COUNTRY = "key_country"
        const val CHANNEL_ID = "channel_severe_weather_alerts"
        const val NOTIFICATION_ID = 9001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting SevereWeatherWorker execution...")
            val lat = inputData.getDouble(KEY_LATITUDE, 37.7749)
            val lon = inputData.getDouble(KEY_LONGITUDE, -122.4194)
            val cityName = inputData.getString(KEY_CITY_NAME) ?: "San Francisco"
            val country = inputData.getString(KEY_COUNTRY) ?: "United States"

            val city = GeocodingResult(
                name = cityName,
                latitude = lat,
                longitude = lon,
                country = country
            )

            val repository = WeatherRepository()
            val weatherData = repository.fetchWeather(city)
            val activeAlerts = weatherData.activeAlerts

            if (activeAlerts.isNotEmpty()) {
                val primaryAlert = activeAlerts.first()
                Log.w(TAG, "Severe weather alert detected: ${primaryAlert.headline}")

                // Post alert to shared state manager for immediate UI display
                AlertStateManager.postAlert(primaryAlert)

                // Trigger high priority system notification if critical
                showSevereAlertNotification(context, primaryAlert)

                Result.success(
                    workDataOf(
                        "alerts_found" to true,
                        "alert_count" to activeAlerts.size,
                        "alert_headline" to primaryAlert.headline
                    )
                )
            } else {
                Log.d(TAG, "No active severe alerts detected for $cityName")
                AlertStateManager.postAlert(null)
                Result.success(
                    workDataOf(
                        "alerts_found" to false,
                        "alert_count" to 0
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing SevereWeatherWorker: ${e.message}", e)
            Result.retry()
        }
    }

    private fun showSevereAlertNotification(context: Context, alert: SevereWeatherAlert) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Create notification channel for Android 8.0+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
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

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
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

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted, skipping notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display severe alert notification: ${e.message}")
        }
    }
}
