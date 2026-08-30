package com.example.data.alert

import com.example.data.model.SevereWeatherAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared singleton manager for active severe weather alerts.
 * Bridges WorkManager background checks and ViewModel UI state.
 */
object AlertStateManager {
    private val _latestAlert = MutableStateFlow<SevereWeatherAlert?>(null)
    val latestAlert: StateFlow<SevereWeatherAlert?> = _latestAlert.asStateFlow()

    private val _lastCheckedTimestamp = MutableStateFlow<Long>(0L)
    val lastCheckedTimestamp: StateFlow<Long> = _lastCheckedTimestamp.asStateFlow()

    fun postAlert(alert: SevereWeatherAlert?) {
        _latestAlert.value = alert
        _lastCheckedTimestamp.value = System.currentTimeMillis()
    }

    fun clearAlert() {
        _latestAlert.value = null
    }
}
