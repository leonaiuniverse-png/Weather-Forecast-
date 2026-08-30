package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GeocodingResult
import com.example.data.repository.CompleteWeatherData
import com.example.data.repository.WeatherRepository
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
    val isAiRefreshing: Boolean = false
)

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(WeatherScreenState())
    val state: StateFlow<WeatherScreenState> = _state.asStateFlow()

    private var searchDebounceJob: Job? = null

    init {
        loadWeather(_state.value.selectedCity)
    }

    fun loadWeather(city: GeocodingResult) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    uiState = if (it.uiState is WeatherUiState.Success) it.uiState else WeatherUiState.Loading,
                    selectedCity = city,
                    isRefreshing = true
                )
            }
            try {
                val weatherData = repository.fetchWeather(city)
                _state.update {
                    it.copy(
                        uiState = WeatherUiState.Success(weatherData),
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        uiState = WeatherUiState.Error(e.message ?: "Failed to fetch weather data"),
                        isRefreshing = false
                    )
                }
            }
        }
    }

    fun refresh() {
        loadWeather(_state.value.selectedCity)
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

    fun selectCity(city: GeocodingResult) {
        closeSearch()
        loadWeather(city)
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
}
