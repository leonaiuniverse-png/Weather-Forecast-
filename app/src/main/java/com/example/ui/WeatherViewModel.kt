package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.alert.AlertStateManager
import com.example.data.location.LocationHelper
import com.example.data.model.GeocodingResult
import com.example.data.model.SevereWeatherAlert
import com.example.data.repository.CompleteWeatherData
import com.example.data.repository.WeatherRepository
import com.example.data.worker.AlertsWorkScheduler
import com.example.data.worker.WeatherWorkScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val data: CompleteWeatherData) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

data class WeatherScreenState(
    val uiState: WeatherUiState = WeatherUiState.Loading,
    val selectedCity: GeocodingResult = WeatherRepository.POPULAR_CITIES[0], // San Francisco
    val isFahrenheit: Boolean = false,
    val isSearchDialogOpen: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<GeocodingResult> = emptyList(),
    val isRefreshing: Boolean = false,
    val isAiRefreshing: Boolean = false,
    val blurIntensity: Float = 0.65f,
    val isSettingsDialogOpen: Boolean = false,
    val isVoiceAssistantOpen: Boolean = false,
    val voiceQuestion: String = "",
    val isListening: Boolean = false,
    val isGeminiAnswering: Boolean = false,
    val geminiAnswer: String? = null,
    val lastAnsweredQuestion: String? = null,
    val isGpsLocating: Boolean = false,
    val isGpsDetected: Boolean = false,
    val gpsErrorMessage: String? = null,
    val activeAlert: SevereWeatherAlert? = null,
    val isAlertDismissed: Boolean = false,
    val isAlertDetailsOpen: Boolean = false,
    val selectedDetailAlert: SevereWeatherAlert? = null,
    val lastSyncTimestamp: Long? = null,
    val isBackgroundSyncConfigured: Boolean = true,
    val isSolarThemeAuto: Boolean = true,
    val manualDiurnalPhase: com.example.ui.theme.DiurnalSolarPhase? = null
)

class WeatherViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WeatherRepository = WeatherRepository()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(WeatherScreenState())
    val state: StateFlow<WeatherScreenState> = _state.asStateFlow()

    private var searchDebounceJob: Job? = null
    private var cacheObserveJob: Job? = null

    init {
        // 1. Instantly hydrate from SQLite / Room local storage cache on app launch
        hydrateFromLocalCache()

        // 2. Observe reactive Room cache and background alerts
        observeRoomCache(getApplication(), _state.value.selectedCity)
        observeAlertStateManager()

        // 3. Initiate live background network sync to update cache
        loadWeather(_state.value.selectedCity, isGps = false, context = getApplication())
    }

    private fun hydrateFromLocalCache() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val cached = repository.getCachedWeather(app, _state.value.selectedCity)
                ?: repository.getLatestCachedWeather(app)
            if (cached != null && _state.value.uiState !is WeatherUiState.Success) {
                _state.update {
                    it.copy(
                        uiState = WeatherUiState.Success(cached),
                        selectedCity = cached.city,
                        lastSyncTimestamp = cached.lastSyncTimestamp
                    )
                }
            }
        }
    }

    private fun observeAlertStateManager() {
        viewModelScope.launch {
            AlertStateManager.latestAlert.collect { incomingAlert ->
                _state.update { current ->
                    // If incoming alert is new or changed, reset dismissal so user sees critical update
                    val isNewAlert = incomingAlert != null && incomingAlert.id != current.activeAlert?.id
                    current.copy(
                        activeAlert = incomingAlert,
                        isAlertDismissed = if (isNewAlert) false else current.isAlertDismissed
                    )
                }
            }
        }
    }

    fun initializeBackgroundWorker(context: Context) {
        // Schedule periodic 15-minute background sync for weather & alerts
        WeatherWorkScheduler.schedulePeriodicWeatherSync(context, _state.value.selectedCity)
        AlertsWorkScheduler.schedulePeriodicChecks(context, _state.value.selectedCity)

        // Observe Room DB updates so that background worker updates are immediately reflected on dashboard
        observeRoomCache(context, _state.value.selectedCity)

        // Hydrate from Room cache immediately if UI is currently loading
        viewModelScope.launch {
            val cached = repository.getCachedWeather(context, _state.value.selectedCity)
            if (cached != null && _state.value.uiState !is WeatherUiState.Success) {
                _state.update {
                    it.copy(
                        uiState = WeatherUiState.Success(cached),
                        lastSyncTimestamp = cached.lastSyncTimestamp
                    )
                }
            }
        }
    }

    private fun observeRoomCache(context: Context, city: GeocodingResult) {
        cacheObserveJob?.cancel()
        cacheObserveJob = viewModelScope.launch {
            repository.observeCachedWeather(context, city).collect { cachedData ->
                if (cachedData != null) {
                    _state.update { current ->
                        if (current.selectedCity.name.equals(cachedData.city.name, ignoreCase = true)) {
                            current.copy(
                                uiState = WeatherUiState.Success(cachedData),
                                lastSyncTimestamp = cachedData.lastSyncTimestamp
                            )
                        } else {
                            current
                        }
                    }
                }
            }
        }
    }

    fun triggerBackgroundSync(context: Context) {
        WeatherWorkScheduler.triggerImmediateSync(context, _state.value.selectedCity)
        AlertsWorkScheduler.triggerImmediateCheck(context, _state.value.selectedCity)
    }

    fun triggerBackgroundAlertCheck(context: Context) {
        AlertsWorkScheduler.triggerImmediateCheck(context, _state.value.selectedCity)
    }

    fun dismissAlert() {
        _state.update { it.copy(isAlertDismissed = true) }
    }

    fun restoreAlert() {
        _state.update { it.copy(isAlertDismissed = false) }
    }

    fun openAlertDetails(alert: SevereWeatherAlert? = null) {
        val target = alert ?: _state.value.activeAlert
        _state.update {
            it.copy(
                isAlertDetailsOpen = true,
                selectedDetailAlert = target
            )
        }
    }

    fun closeAlertDetails() {
        _state.update {
            it.copy(
                isAlertDetailsOpen = false,
                selectedDetailAlert = null
            )
        }
    }

    fun simulateSevereAlert(alert: SevereWeatherAlert) {
        AlertStateManager.postAlert(alert)
        _state.update { it.copy(activeAlert = alert, isAlertDismissed = false) }
    }

    fun clearActiveAlert() {
        AlertStateManager.clearAlert()
        _state.update { it.copy(activeAlert = null, isAlertDismissed = false) }
    }

    fun loadWeather(city: GeocodingResult, isGps: Boolean = false, context: Context? = null) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    uiState = if (it.uiState is WeatherUiState.Success) it.uiState else WeatherUiState.Loading,
                    selectedCity = city,
                    isGpsDetected = isGps,
                    isRefreshing = true,
                    gpsErrorMessage = null
                )
            }

            // Immediately check Room cache for instant rendering
            if (context != null) {
                val cached = repository.getCachedWeather(context, city)
                if (cached != null) {
                    _state.update {
                        it.copy(
                            uiState = WeatherUiState.Success(cached),
                            lastSyncTimestamp = cached.lastSyncTimestamp
                        )
                    }
                }
                observeRoomCache(context, city)
                WeatherWorkScheduler.schedulePeriodicWeatherSync(context, city)
            }

            try {
                val weatherData = repository.fetchWeather(city)
                _state.update {
                    it.copy(
                        uiState = WeatherUiState.Success(weatherData),
                        isRefreshing = false,
                        lastSyncTimestamp = weatherData.lastSyncTimestamp
                    )
                }

                // Persist fresh data to Room Database cache
                if (context != null) {
                    repository.cacheWeatherData(context, weatherData, syncSource = "FOREGROUND")
                }
            } catch (e: Exception) {
                // If we already have a success UI from cache, keep it but stop refreshing
                _state.update {
                    if (it.uiState is WeatherUiState.Success) {
                        it.copy(isRefreshing = false)
                    } else {
                        it.copy(
                            uiState = WeatherUiState.Error(e.message ?: "Failed to fetch weather data"),
                            isRefreshing = false
                        )
                    }
                }
            }
        }
    }

    fun detectAndLoadGpsWeather(context: Context, locationHelper: LocationHelper = LocationHelper(context)) {
        viewModelScope.launch {
            _state.update { it.copy(isGpsLocating = true, gpsErrorMessage = null) }
            try {
                val location = locationHelper.getCurrentGpsLocation()
                if (location != null) {
                    val resolvedCity = locationHelper.reverseGeocode(location.latitude, location.longitude)
                    _state.update {
                        it.copy(
                            isGpsLocating = false,
                            isGpsDetected = true,
                            gpsErrorMessage = null
                        )
                    }
                    loadWeather(resolvedCity, isGps = true, context = context)
                } else {
                    _state.update {
                        it.copy(
                            isGpsLocating = false,
                            gpsErrorMessage = if (!locationHelper.hasLocationPermission()) {
                                "Location permission not granted"
                            } else {
                                "Unable to acquire GPS fix. Please ensure location services are enabled."
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isGpsLocating = false,
                        gpsErrorMessage = "GPS Detection error: ${e.message}"
                    )
                }
            }
        }
    }

    fun refresh(context: Context? = null) {
        loadWeather(_state.value.selectedCity, isGps = _state.value.isGpsDetected, context = context)
    }

    fun toggleUnit() {
        _state.update { it.copy(isFahrenheit = !it.isFahrenheit) }
    }

    fun openSearch() {
        _state.update { it.copy(isSearchDialogOpen = true, searchQuery = "", searchResults = emptyList()) }
    }

    fun closeSearch() {
        _state.update { it.copy(isSearchDialogOpen = false, searchQuery = "", searchResults = emptyList()) }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchDebounceJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchDebounceJob = viewModelScope.launch {
            delay(350)
            _state.update { it.copy(isSearching = true) }
            val results = repository.searchCities(query)
            _state.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun selectCity(city: GeocodingResult, context: Context? = null) {
        closeSearch()
        loadWeather(city, isGps = false, context = context)
    }

    fun convertTemp(tempC: Double): Int {
        return if (_state.value.isFahrenheit) {
            (tempC * 9.0 / 5.0 + 32.0).toInt()
        } else {
            tempC.toInt()
        }
    }

    fun getUnitSymbol(): String {
        return if (_state.value.isFahrenheit) "°F" else "°C"
    }

    fun getSpeedString(kmh: Double): String {
        return if (_state.value.isFahrenheit) {
            val mph = kmh * 0.621371
            "${mph.toInt()} mph"
        } else {
            "${kmh.toInt()} km/h"
        }
    }

    fun getPrecipitationString(mm: Double): String {
        if (mm <= 0.0) return "No rain"
        return if (_state.value.isFahrenheit) {
            val inches = mm * 0.0393701
            String.format(java.util.Locale.US, "%.2f in", inches)
        } else {
            "$mm mm"
        }
    }

    fun updateBlurIntensity(intensity: Float) {
        _state.update { it.copy(blurIntensity = intensity.coerceIn(0.0f, 1.0f)) }
    }

    fun openSettings() {
        _state.update { it.copy(isSettingsDialogOpen = true) }
    }

    fun closeSettings() {
        _state.update { it.copy(isSettingsDialogOpen = false) }
    }

    fun openVoiceAssistant(initialQuery: String? = null) {
        _state.update {
            it.copy(
                isVoiceAssistantOpen = true,
                voiceQuestion = initialQuery ?: "",
                isListening = false
            )
        }
        if (!initialQuery.isNullOrBlank()) {
            askGemini(initialQuery)
        }
    }

    fun closeVoiceAssistant() {
        _state.update {
            it.copy(
                isVoiceAssistantOpen = false,
                isListening = false
            )
        }
    }

    fun updateVoiceQuestion(question: String) {
        _state.update { it.copy(voiceQuestion = question) }
    }

    fun setListening(isListening: Boolean) {
        _state.update { it.copy(isListening = isListening) }
    }

    fun askGemini(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) return

        val currentWeather = (_state.value.uiState as? WeatherUiState.Success)?.data ?: return
        val isFahrenheit = _state.value.isFahrenheit

        viewModelScope.launch {
            _state.update {
                it.copy(
                    voiceQuestion = trimmed,
                    isListening = false,
                    isGeminiAnswering = true,
                    lastAnsweredQuestion = trimmed
                )
            }
            try {
                val answer = repository.askGeminiWeatherQuestion(
                    question = trimmed,
                    weather = currentWeather,
                    isFahrenheit = isFahrenheit
                )
                _state.update {
                    it.copy(
                        geminiAnswer = answer,
                        isGeminiAnswering = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        geminiAnswer = "Unable to process Gemini weather inquiry: ${e.message}",
                        isGeminiAnswering = false
                    )
                }
            }
        }
    }

    fun setSolarThemeAuto(enabled: Boolean) {
        _state.update {
            it.copy(
                isSolarThemeAuto = enabled,
                manualDiurnalPhase = if (enabled) null else (it.manualDiurnalPhase ?: com.example.ui.theme.DiurnalSolarPhase.GOLDEN_SUNRISE)
            )
        }
    }

    fun setManualDiurnalPhase(phase: com.example.ui.theme.DiurnalSolarPhase?) {
        _state.update {
            it.copy(
                isSolarThemeAuto = phase == null,
                manualDiurnalPhase = phase
            )
        }
    }

    fun getDiurnalThemeState(): com.example.ui.theme.DiurnalThemeState {
        val currentState = _state.value
        val weather = (currentState.uiState as? WeatherUiState.Success)?.data
        val sunCycle = weather?.sunCycle
        val conditionType = weather?.condition?.type ?: com.example.data.model.WeatherConditionType.CLEAR_DAY
        val isDay = weather?.condition?.isDay ?: true

        val activePhase = if (currentState.isSolarThemeAuto || currentState.manualDiurnalPhase == null) {
            com.example.ui.theme.SolarThemeScheduler.calculateDiurnalPhase(sunCycle)
        } else {
            currentState.manualDiurnalPhase
        }

        val (nextPhase, nextSummary) = com.example.ui.theme.SolarThemeScheduler.calculateNextTransition(activePhase, sunCycle)
        val palette = com.example.ui.theme.SolarThemeScheduler.generatePalette(
            phase = activePhase,
            condition = conditionType,
            isDay = isDay
        )

        return com.example.ui.theme.DiurnalThemeState(
            phase = activePhase,
            solarProgress = sunCycle?.solarProgress ?: 0.5f,
            nextPhase = nextPhase,
            nextTransitionSummary = nextSummary,
            isAutoScheduleEnabled = currentState.isSolarThemeAuto,
            palette = palette
        )
    }
}

