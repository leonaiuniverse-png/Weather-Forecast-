package com.example.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.location.LocationHelper
import com.example.data.model.DailyForecastItem
import com.example.data.model.HourlyForecastItem
import com.example.data.model.WeatherConditionType
import com.example.data.repository.CompleteWeatherData
import com.example.ui.components.AtmosphericBackground
import com.example.ui.components.DismissedAlertMiniIndicator
import com.example.ui.components.FeelsLikeTrendCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassSettingsDialog
import com.example.ui.components.GlassShareFloatingButton
import com.example.ui.components.GlassVoiceAssistantDialog
import com.example.ui.components.LocalGlassBlurIntensity
import com.example.ui.components.MinMaxThermometerBar
import com.example.ui.components.MoistureWaveIndicator
import com.example.ui.components.PulsingSparkle
import com.example.ui.components.SearchCityDialog
import com.example.ui.components.SevereWeatherAlertBanner
import com.example.ui.components.SevereWeatherAlertDetailsDialog
import com.example.ui.components.SunCycleCard
import com.example.ui.components.UvGaugeBar
import com.example.ui.components.WeatherCondition3DIcon
import com.example.ui.components.WindCompassRose
import com.example.ui.components.getWeatherIconData
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.GlassBorderEnd
import com.example.ui.theme.GlassBorderStart
import com.example.ui.theme.LocalGlassTypography
import com.example.ui.theme.SkyGlassWeatherTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import com.example.ui.util.HapticUtils
import com.example.ui.util.ShareUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Permission launcher for Location APIs
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.detectAndLoadGpsWeather(context)
        }
    }

    // Auto-detect user's GPS coordinates on startup & schedule background severe weather alert worker
    LaunchedEffect(Unit) {
        // Initialize background worker for severe weather alerts monitoring
        viewModel.initializeBackgroundWorker(context)

        val locationHelper = LocationHelper(context)
        if (locationHelper.hasLocationPermission()) {
            viewModel.detectAndLoadGpsWeather(context, locationHelper)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val currentConditionType = when (val s = state.uiState) {
        is WeatherUiState.Success -> s.data.condition.type
        else -> WeatherConditionType.CLEAR_DAY
    }
    val isDay = when (val s = state.uiState) {
        is WeatherUiState.Success -> s.data.condition.isDay
        else -> true
    }

    // Subtle tactile haptic trigger on dashboard data load & update
    LaunchedEffect(state.uiState) {
        if (state.uiState is WeatherUiState.Success) {
            HapticUtils.performUpdateSuccess(view)
        }
    }

    val diurnalThemeState = viewModel.getDiurnalThemeState()
    val pullToRefreshState = rememberPullToRefreshState()

    SkyGlassWeatherTheme(
        diurnalPalette = diurnalThemeState.palette,
        diurnalState = diurnalThemeState
    ) {
        CompositionLocalProvider(
            LocalGlassBlurIntensity provides state.blurIntensity
        ) {
            AtmosphericBackground(
                conditionType = currentConditionType,
                isDay = isDay,
                diurnalPalette = diurnalThemeState.palette,
                modifier = Modifier.fillMaxSize()
            ) {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = {
                            HapticUtils.performRefresh(view)
                            viewModel.refresh(context)
                            viewModel.triggerBackgroundSync(context)
                        },
                        state = pullToRefreshState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = statusBarPadding, bottom = navBarPadding),
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                state = pullToRefreshState,
                                isRefreshing = state.isRefreshing,
                                modifier = Modifier.align(Alignment.TopCenter),
                                containerColor = Color(0xD00B132B),
                                color = AccentCyan
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp)
                                .widthIn(max = 600.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 1. Header Bar with GPS status & action
                HeaderBar(
                    cityName = state.selectedCity.name,
                    countryCode = state.selectedCity.countryCode ?: state.selectedCity.country,
                    isFahrenheit = state.isFahrenheit,
                    isGpsDetected = state.isGpsDetected,
                    isGpsLocating = state.isGpsLocating,
                    onSearchClick = {
                        HapticUtils.performClick(view)
                        viewModel.openSearch()
                    },
                    onLocateClick = {
                        HapticUtils.performClick(view)
                        val locationHelper = LocationHelper(context)
                        if (locationHelper.hasLocationPermission()) {
                            viewModel.detectAndLoadGpsWeather(context, locationHelper)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onToggleUnit = {
                        HapticUtils.performTick(view)
                        viewModel.toggleUnit()
                    },
                    onSettingsClick = {
                        HapticUtils.performClick(view)
                        viewModel.openSettings()
                    },
                    onVoiceClick = {
                        HapticUtils.performClick(view)
                        viewModel.openVoiceAssistant()
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Critical High-Priority Severe Weather Alert Banner (when active)
                if (state.activeAlert != null) {
                    if (!state.isAlertDismissed) {
                        SevereWeatherAlertBanner(
                            alert = state.activeAlert!!,
                            onViewDetails = {
                                HapticUtils.performClick(view)
                                viewModel.openAlertDetails(state.activeAlert)
                            },
                            onDismiss = {
                                HapticUtils.performTick(view)
                                viewModel.dismissAlert()
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    } else {
                        DismissedAlertMiniIndicator(
                            alert = state.activeAlert!!,
                            onClick = {
                                HapticUtils.performClick(view)
                                viewModel.restoreAlert()
                                viewModel.openAlertDetails(state.activeAlert)
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                when (val uiState = state.uiState) {
                is WeatherUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = AccentCyan,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Calibrating Sky Telemetry...",
                                color = TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                is WeatherUiState.Error -> {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        cornerRadius = 24.dp,
                        borderGradient = Brush.linearGradient(listOf(AccentRose.copy(alpha = 0.6f), GlassBorderEnd))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Grain,
                                contentDescription = null,
                                tint = AccentRose,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Connection Interrupted",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.message,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassCard(
                                cornerRadius = 14.dp,
                                onClick = {
                                    HapticUtils.performClick(view)
                                    viewModel.refresh()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Retry",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Retry Live Sync",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                is WeatherUiState.Success -> {
                    val weather = uiState.data

                    // 2. Hero Weather Showcase
                    HeroWeatherShowcase(
                        weather = weather,
                        isFahrenheit = state.isFahrenheit,
                        convertTemp = { viewModel.convertTemp(it) },
                        unitSymbol = viewModel.getUnitSymbol()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. AI "Sky Intelligence" Glass Banner
                    SkyIntelligenceBanner(
                        briefing = weather.aiBriefing,
                        onAskVoiceAssistant = {
                            HapticUtils.performClick(view)
                            viewModel.openVoiceAssistant()
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Bento Grid Sensor Row (2x2 Glass Cards)
                    BentoGridSensorRow(
                        weather = weather,
                        convertTemp = { viewModel.convertTemp(it) },
                        unitSymbol = viewModel.getUnitSymbol(),
                        getSpeedString = { viewModel.getSpeedString(it) },
                        getPrecipitationString = { viewModel.getPrecipitationString(it) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 5. Sun Cycle Glass Card
                    SunCycleCard(
                        sunCycle = weather.sunCycle
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 6. 24-Hour Forecast Horizontal Strip
                    HourlyForecastStrip(
                        hourlyList = weather.hourlyList,
                        convertTemp = { viewModel.convertTemp(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 6b. Feels Like 24-Hour Trend Line Graph (Secondary Chart)
                    FeelsLikeTrendCard(
                        hourlyList = weather.hourlyList,
                        currentTempC = weather.currentTempC,
                        apparentTempC = weather.apparentTempC,
                        convertTemp = { viewModel.convertTemp(it) },
                        unitSymbol = viewModel.getUnitSymbol()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 7. 7-Day Precision Forecast
                    PrecisionDailyForecast(
                        dailyList = weather.dailyList,
                        convertTemp = { viewModel.convertTemp(it) }
                    )

                    // Bottom clearance for floating action button
                    Spacer(modifier = Modifier.height(84.dp))
                }
            }
        }
    }

    // Floating Action Button to share weather dashboard snapshot via native share sheet
    if (state.uiState is WeatherUiState.Success) {
        val weather = (state.uiState as WeatherUiState.Success).data
        GlassShareFloatingButton(
            onClick = {
                HapticUtils.performClick(view)
                val tempFormatted = "${viewModel.convertTemp(weather.currentTempC)}${viewModel.getUnitSymbol()}"
                ShareUtils.shareWeatherDashboard(
                    context = context,
                    view = view,
                    cityName = state.selectedCity.name,
                    tempFormatted = tempFormatted,
                    conditionTitle = weather.condition.title
                )
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = navBarPadding + 16.dp)
        )
    }

    // Search Modal Dialog
    if (state.isSearchDialogOpen) {
        SearchCityDialog(
            searchQuery = state.searchQuery,
            onQueryChanged = { viewModel.onSearchQueryChanged(it) },
            isSearching = state.isSearching,
            searchResults = state.searchResults,
            isGpsLocating = state.isGpsLocating,
            onCitySelected = { viewModel.selectCity(it, context) },
            onUseCurrentLocation = {
                val locationHelper = LocationHelper(context)
                if (locationHelper.hasLocationPermission()) {
                    viewModel.detectAndLoadGpsWeather(context, locationHelper)
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            onDismiss = { viewModel.closeSearch() }
        )
    }

    // Glassmorphism Settings Modal Dialog
    if (state.isSettingsDialogOpen) {
        GlassSettingsDialog(
            blurIntensity = state.blurIntensity,
            onIntensityChange = { viewModel.updateBlurIntensity(it) },
            onDismiss = { viewModel.closeSettings() },
            onTriggerSyncNow = {
                viewModel.triggerBackgroundSync(context)
            },
            lastSyncTimestamp = state.lastSyncTimestamp,
            cityName = state.selectedCity.name,
            isSolarThemeAuto = state.isSolarThemeAuto,
            manualDiurnalPhase = state.manualDiurnalPhase,
            onSolarThemeAutoChange = { viewModel.setSolarThemeAuto(it) },
            onManualDiurnalPhaseChange = { viewModel.setManualDiurnalPhase(it) }
        )
    }

    // Voice Assistant & Gemini Q&A Modal Dialog
    if (state.isVoiceAssistantOpen) {
        GlassVoiceAssistantDialog(
            cityName = state.selectedCity.name,
            question = state.voiceQuestion,
            isListening = state.isListening,
            isGeminiAnswering = state.isGeminiAnswering,
            geminiAnswer = state.geminiAnswer,
            lastAnsweredQuestion = state.lastAnsweredQuestion,
            onQuestionChange = { viewModel.updateVoiceQuestion(it) },
            onListeningChange = { viewModel.setListening(it) },
            onAskGemini = { viewModel.askGemini(it) },
            onDismiss = { viewModel.closeVoiceAssistant() }
        )
    }

    // Severe Weather Alert Details & Safety Directives Dialog
    if (state.isAlertDetailsOpen && state.selectedDetailAlert != null) {
        SevereWeatherAlertDetailsDialog(
            alert = state.selectedDetailAlert!!,
            onDismiss = {
                HapticUtils.performTick(view)
                viewModel.closeAlertDetails()
            }
        )
    }
}
}
}
}

/**
 * 1. Header Bar
 */
@Composable
fun HeaderBar(
    cityName: String,
    countryCode: String?,
    isFahrenheit: Boolean,
    isGpsDetected: Boolean = false,
    isGpsLocating: Boolean = false,
    onSearchClick: () -> Unit,
    onLocateClick: () -> Unit = {},
    onToggleUnit: () -> Unit,
    onSettingsClick: () -> Unit,
    onVoiceClick: () -> Unit
) {
    val typo = LocalGlassTypography.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Location Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onSearchClick)
                .padding(vertical = 4.dp, horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (isGpsDetected) AccentCyan.copy(alpha = 0.22f) else AccentAmber.copy(alpha = 0.18f),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isGpsDetected) AccentCyan.copy(alpha = 0.45f) else AccentAmber.copy(alpha = 0.35f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isGpsLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AccentCyan
                    )
                } else if (isGpsDetected) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = "GPS Location Active",
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "Location Pin",
                        tint = AccentAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cityName,
                        color = TextPrimary,
                        fontSize = (19 * min(typo.fontScale, 1.25f)).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isGpsDetected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(AccentCyan.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                .border(0.8.dp, AccentCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "GPS",
                                color = AccentCyan,
                                style = typo.badgeText.copy(
                                    fontSize = (9.5f * min(typo.fontScale, 1.3f)).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else if (!countryCode.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = countryCode.uppercase(),
                                color = AccentCyan,
                                style = typo.badgeText.copy(fontSize = (9.5f * min(typo.fontScale, 1.3f)).sp)
                            )
                        }
                    }
                }
                val todayDateStr = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
                Text(
                    text = todayDateStr,
                    style = typo.subText
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Action Buttons (Unit Toggle + Settings + GPS + Search + Refresh)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Unit Toggle Pill (°C / °F with clear metric/imperial indication)
            GlassCard(
                cornerRadius = 18.dp,
                onClick = onToggleUnit,
                modifier = Modifier.testTag("unit_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isFahrenheit) AccentCyan.copy(alpha = 0.25f) else Color.Transparent)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "°C",
                            color = if (!isFahrenheit) AccentCyan else TextMuted,
                            style = typo.badgeText.copy(
                                fontWeight = if (!isFahrenheit) FontWeight.Bold else FontWeight.Normal,
                                fontSize = (11f * min(typo.fontScale, 1.25f)).sp
                            )
                        )
                    }
                    Text(
                        text = "|",
                        color = GlassBorderStart,
                        fontSize = 11.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isFahrenheit) AccentCyan.copy(alpha = 0.25f) else Color.Transparent)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "°F",
                            color = if (isFahrenheit) AccentCyan else TextMuted,
                            style = typo.badgeText.copy(
                                fontWeight = if (isFahrenheit) FontWeight.Bold else FontWeight.Normal,
                                fontSize = (11f * min(typo.fontScale, 1.25f)).sp
                            )
                        )
                    }
                }
            }

            // GPS Locate Quick Action Button
            GlassCard(
                cornerRadius = 20.dp,
                onClick = onLocateClick,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("gps_locate_button")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isGpsLocating) {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "Detect GPS Coordinates",
                            tint = if (isGpsDetected) AccentCyan else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Glassmorphism Blur Settings Button
            GlassCard(
                cornerRadius = 20.dp,
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("settings_button")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Glass Settings",
                        tint = AccentCyan,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Gemini Voice Assistant Button
            GlassCard(
                cornerRadius = 20.dp,
                onClick = onVoiceClick,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("voice_assistant_button")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Ask Gemini Voice Assistant",
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Search Button
            GlassCard(
                cornerRadius = 20.dp,
                onClick = onSearchClick,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("search_button")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search City",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 2. Hero Weather Showcase
 */
@Composable
fun HeroWeatherShowcase(
    weather: CompleteWeatherData,
    isFahrenheit: Boolean,
    convertTemp: (Double) -> Int,
    unitSymbol: String
) {
    val typo = LocalGlassTypography.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered 3D-styled animated condition icon
        WeatherCondition3DIcon(
            weatherCode = weather.weatherCode,
            isDay = weather.condition.isDay
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Massive Display Temperature with optical non-linear scale clamping
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${convertTemp(weather.currentTempC)}",
                color = TextPrimary,
                style = typo.heroTemperature
            )
            Text(
                text = unitSymbol,
                color = AccentCyan,
                style = typo.heroUnit,
                modifier = Modifier.padding(top = (10 * min(typo.fontScale, 1.2f)).dp)
            )
        }

        // Weather Condition Label
        Text(
            text = weather.condition.title,
            color = TextPrimary,
            style = typo.conditionHeadline,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Feels like & High/Low Pills
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Feels like ${convertTemp(weather.apparentTempC)}$unitSymbol",
                style = typo.subText.copy(fontSize = (13.5f * min(typo.fontScale, 1.25f)).sp)
            )
            Text(text = "•", color = TextMuted, fontSize = 12.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "H: ${convertTemp(weather.todayMaxTempC)}°",
                    color = AccentAmber,
                    style = typo.subText.copy(fontWeight = FontWeight.Medium, color = AccentAmber, fontSize = (13.5f * min(typo.fontScale, 1.25f)).sp)
                )
                Text(
                    text = "L: ${convertTemp(weather.todayMinTempC)}°",
                    color = AccentCyan,
                    style = typo.subText.copy(fontWeight = FontWeight.Medium, color = AccentCyan, fontSize = (13.5f * min(typo.fontScale, 1.25f)).sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Background WorkManager Sync Status Indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(if (weather.isFromCache) AccentAmber else AccentCyan, CircleShape)
            )
            Text(
                text = if (weather.isFromCache) "WorkManager Cached • 15m Periodic" else "Live Telemetry • WorkManager Synced",
                color = Color.White.copy(alpha = 0.7f),
                style = typo.subText.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal)
            )
        }
    }
}

/**
 * 3. AI "Sky Intelligence" Glass Banner
 */
@Composable
fun SkyIntelligenceBanner(
    briefing: String,
    onAskVoiceAssistant: () -> Unit
) {
    val typo = LocalGlassTypography.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sky_intelligence_banner"),
        cornerRadius = 24.dp,
        borderGradient = Brush.linearGradient(
            colors = listOf(AccentCyan.copy(alpha = 0.55f), AccentAmber.copy(alpha = 0.25f), GlassBorderEnd)
        ),
        fillGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF00F2FE).copy(alpha = 0.09f),
                Color(0xFF1E293B).copy(alpha = 0.25f),
                Color(0xFF0F172A).copy(alpha = 0.35f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row with Pulsing Sparkle & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PulsingSparkle(color = AccentCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SKY INTELLIGENCE",
                    style = typo.sectionHeader,
                    color = AccentCyan
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GEMINI FLASH",
                        color = Color.White.copy(alpha = 0.85f),
                        style = typo.badgeText.copy(fontSize = (8.5f * min(typo.fontScale, 1.3f)).sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Conversational adaptive text
            Text(
                text = briefing,
                style = typo.bodyBriefing
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Voice Assistant Prompt Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, AccentCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onAskVoiceAssistant)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Ask Gemini: \"Will it rain today?\"",
                        color = TextPrimary,
                        style = typo.badgeText.copy(
                            fontSize = (12f * min(typo.fontScale, 1.25f)).sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 4. Bento Grid Sensor Row (2x2 Glass Cards)
 */
@Composable
fun BentoGridSensorRow(
    weather: CompleteWeatherData,
    convertTemp: (Double) -> Int,
    unitSymbol: String,
    getSpeedString: (Double) -> String,
    getPrecipitationString: (Double) -> String
) {
    val typo = LocalGlassTypography.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Row 1: Humidity & Wind Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 1: Humidity (%)
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("sensor_humidity_card"),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HUMIDITY",
                            style = typo.metricLabel
                        )
                        Icon(
                            imageVector = Icons.Rounded.Opacity,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${weather.humidityPct}%",
                        style = typo.metricValue
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    MoistureWaveIndicator(humidityPct = weather.humidityPct)

                    Spacer(modifier = Modifier.height(6.dp))

                    // Dew point estimation
                    val dewPoint = weather.currentTempC - ((100 - weather.humidityPct) / 5.0)
                    Text(
                        text = "Dew point ${convertTemp(dewPoint)}$unitSymbol",
                        style = typo.subText
                    )
                }
            }

            // Card 2: Wind Speed
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("sensor_wind_card"),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WIND SPEED",
                            style = typo.metricLabel
                        )
                        Icon(
                            imageVector = Icons.Rounded.Air,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = getSpeedString(weather.windSpeedKmh),
                        style = typo.metricValue.copy(fontSize = (22 * min(typo.fontScale, 1.25f)).sp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WindCompassRose(speedKmh = weather.windSpeedKmh)
                        Column {
                            val gustKmh = weather.windSpeedKmh * 1.45
                            Text(
                                text = "Gusts to ${getSpeedString(gustKmh)}",
                                style = typo.subText
                            )
                            Text(
                                text = if (weather.windSpeedKmh > 25) "Brisk flow" else "Gentle breeze",
                                style = typo.subText.copy(color = TextMuted, fontSize = (9.5f * min(typo.fontScale, 1.3f)).sp)
                            )
                        }
                    }
                }
            }
        }

        // Row 2: UV Index & Precipitation Probability
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 3: UV Index
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("sensor_uv_card"),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UV INDEX",
                            style = typo.metricLabel
                        )
                        Icon(
                            imageVector = Icons.Rounded.WbSunny,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", weather.currentUvIndex),
                            style = typo.metricValue
                        )
                        val uvTag = when {
                            weather.currentUvIndex < 3.0 -> "Low"
                            weather.currentUvIndex < 6.0 -> "Moderate"
                            weather.currentUvIndex < 8.0 -> "High"
                            else -> "Very High"
                        }
                        val tagColor = when {
                            weather.currentUvIndex < 3.0 -> AccentEmerald
                            weather.currentUvIndex < 6.0 -> AccentAmber
                            weather.currentUvIndex < 8.0 -> Color(0xFFF97316)
                            else -> Color(0xFFA855F7)
                        }
                        Box(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .background(tagColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .border(1.dp, tagColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = uvTag,
                                color = tagColor,
                                style = typo.badgeText.copy(fontSize = (9.5f * min(typo.fontScale, 1.3f)).sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    UvGaugeBar(uvIndex = weather.currentUvIndex)

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (weather.currentUvIndex >= 6.0) "Protection required" else "Minimal sun risk",
                        style = typo.subText
                    )
                }
            }

            // Card 4: Precipitation Probability
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("sensor_precipitation_card"),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRECIPITATION",
                            style = typo.metricLabel
                        )
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${weather.currentPrecipProb}%",
                        style = typo.metricValue
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(AccentCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = getPrecipitationString(weather.precipitationMm),
                            style = typo.subText
                        )
                    }
                }
            }
        }
    }
}

/**
 * 5. 24-Hour Forecast Horizontal Strip
 */
@Composable
fun HourlyForecastStrip(
    hourlyList: List<HourlyForecastItem>,
    convertTemp: (Double) -> Int
) {
    val typo = LocalGlassTypography.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HOURLY FORECAST",
                style = typo.sectionHeader
            )
            Text(
                text = "Next 24 Hours",
                style = typo.timeLabel
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            hourlyList.forEach { item ->
                HourlyPill(item = item, convertTemp = convertTemp)
            }
        }
    }
}

@Composable
fun HourlyPill(
    item: HourlyForecastItem,
    convertTemp: (Double) -> Int
) {
    val typo = LocalGlassTypography.current
    val isCurrent = item.isCurrentHour
    val (iconVector, iconColor, _) = getWeatherIconData(item.weatherCode, isDay = true)

    GlassCard(
        modifier = Modifier
            .widthIn(min = 68.dp)
            .testTag("hourly_pill_${item.hourLabel}"),
        cornerRadius = 20.dp,
        borderGradient = if (isCurrent) {
            Brush.verticalGradient(listOf(AccentCyan.copy(alpha = 0.8f), AccentAmber.copy(alpha = 0.4f)))
        } else {
            Brush.verticalGradient(listOf(GlassBorderStart, GlassBorderEnd))
        },
        fillGradient = if (isCurrent) {
            Brush.verticalGradient(listOf(AccentCyan.copy(alpha = 0.2f), Color(0x15FFFFFF)))
        } else {
            Brush.verticalGradient(listOf(Color(0x1EFFFFFF), Color(0x08FFFFFF)))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.hourLabel,
                color = if (isCurrent) AccentCyan else TextSecondary,
                style = typo.timeLabel.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (item.precipitationProb > 10) {
                Text(
                    text = "${item.precipitationProb}%",
                    color = AccentCyan,
                    style = typo.badgeText.copy(fontSize = (9.5f * min(typo.fontScale, 1.3f)).sp)
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${convertTemp(item.tempC)}°",
                color = TextPrimary,
                style = typo.timeLabel.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (16 * min(typo.fontScale, 1.25f)).sp
                )
            )
        }
    }
}

/**
 * 6. 7-Day Precision Forecast
 */
@Composable
fun PrecisionDailyForecast(
    dailyList: List<DailyForecastItem>,
    convertTemp: (Double) -> Int
) {
    val typo = LocalGlassTypography.current
    val weekMin = dailyList.minOfOrNull { it.minTempC } ?: 10.0
    val weekMax = dailyList.maxOfOrNull { it.maxTempC } ?: 30.0

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("precision_daily_forecast_card"),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "7-DAY PRECISION FORECAST",
                    style = typo.sectionHeader
                )
                Icon(
                    imageVector = Icons.Rounded.Thermostat,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            dailyList.forEachIndexed { index, item ->
                DailyForecastRow(
                    item = item,
                    weekMin = weekMin,
                    weekMax = weekMax,
                    convertTemp = convertTemp
                )
                if (index < dailyList.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun DailyForecastRow(
    item: DailyForecastItem,
    weekMin: Double,
    weekMax: Double,
    convertTemp: (Double) -> Int
) {
    val typo = LocalGlassTypography.current
    val (iconVector, iconColor, _) = getWeatherIconData(item.weatherCode, isDay = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day Label (e.g. Today, Mon, Tue)
        Text(
            text = item.dayLabel,
            color = if (item.dayLabel == "Today") AccentCyan else TextPrimary,
            style = typo.timeLabel.copy(
                fontWeight = if (item.dayLabel == "Today") FontWeight.Bold else FontWeight.Medium,
                fontSize = (13.5f * min(typo.fontScale, 1.25f)).sp
            ),
            modifier = Modifier.widthIn(min = 60.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Weather Icon & POP
        Row(
            modifier = Modifier.widthIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            if (item.precipitationProb > 15) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${item.precipitationProb}%",
                    color = AccentCyan,
                    style = typo.badgeText.copy(fontSize = (9.5f * min(typo.fontScale, 1.3f)).sp)
                )
            }
        }

        // Min Temp
        Text(
            text = "${convertTemp(item.minTempC)}°",
            color = TextSecondary,
            style = typo.timeLabel.copy(
                fontWeight = FontWeight.Medium,
                fontSize = (13.5f * min(typo.fontScale, 1.25f)).sp
            ),
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 28.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Horizontal min-max range gradient bar
        MinMaxThermometerBar(
            minTemp = item.minTempC,
            maxTemp = item.maxTempC,
            weekMin = weekMin,
            weekMax = weekMax,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Max Temp
        Text(
            text = "${convertTemp(item.maxTempC)}°",
            color = TextPrimary,
            style = typo.timeLabel.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (13.5f * min(typo.fontScale, 1.25f)).sp
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier.widthIn(min = 28.dp)
        )
    }
}
