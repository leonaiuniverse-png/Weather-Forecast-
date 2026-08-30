package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.WeatherConditionType
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DiurnalSolarPhase
import com.example.ui.theme.GlassBorderStart
import com.example.ui.theme.LocalGlassTypography
import com.example.ui.theme.SolarThemeScheduler
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.HapticUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlassSettingsDialog(
    blurIntensity: Float,
    onIntensityChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onTriggerSyncNow: (() -> Unit)? = null,
    lastSyncTimestamp: Long? = null,
    cityName: String = "Current City",
    isSolarThemeAuto: Boolean = true,
    manualDiurnalPhase: DiurnalSolarPhase? = null,
    onSolarThemeAutoChange: ((Boolean) -> Unit)? = null,
    onManualDiurnalPhaseChange: ((DiurnalSolarPhase?) -> Unit)? = null
) {
    val typo = LocalGlassTypography.current
    val view = LocalView.current
    val percentage = (blurIntensity * 100).roundToInt()

    val levelDescription = when {
        percentage <= 25 -> "Crisp Crystal (Ultra Clear)"
        percentage <= 50 -> "Light Frost (Subtle Translucency)"
        percentage <= 75 -> "Balanced Glass (Standard Aerogel)"
        else -> "Deep Frosted (Milky Diffusion)"
    }

    val syncTimeFormatted = if (lastSyncTimestamp != null && lastSyncTimestamp > 0) {
        SimpleDateFormat("h:mm a, MMM d", Locale.getDefault()).format(Date(lastSyncTimestamp))
    } else {
        "Just now"
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("glass_settings_dialog"),
            cornerRadius = 28.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header with Title & Dismiss Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AccentCyan.copy(alpha = 0.15f))
                                .border(1.dp, AccentCyan.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Settings & Engine",
                                color = TextPrimary,
                                style = typo.conditionHeadline.copy(fontSize = (18f * min(typo.fontScale, 1.25f)).sp)
                            )
                            Text(
                                text = "Solar Scheduler, WorkManager & Blur",
                                color = TextMuted,
                                style = typo.subText.copy(fontSize = (11f * min(typo.fontScale, 1.25f)).sp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            HapticUtils.performTick(view)
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ----------------------------------------------------
                // 1. SOLAR DIURNAL THEME SCHEDULER SECTION
                // ----------------------------------------------------
                Text(
                    text = "SOLAR DIURNAL THEME SCHEDULER",
                    color = TextMuted,
                    style = typo.sectionHeader
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, AccentAmber.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        // Auto Solar Sync Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(AccentAmber.copy(alpha = 0.20f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WbSunny,
                                        contentDescription = null,
                                        tint = AccentAmber,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(9.dp))
                                Column {
                                    Text(
                                        text = "Auto Sunrise/Sunset Cycle",
                                        color = TextPrimary,
                                        style = typo.badgeText.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    )
                                    Text(
                                        text = "Cycles palette dynamically by solar position",
                                        color = TextSecondary,
                                        style = typo.subText.copy(fontSize = 10.5.sp)
                                    )
                                }
                            }

                            Switch(
                                checked = isSolarThemeAuto,
                                onCheckedChange = { checked ->
                                    HapticUtils.performClick(view)
                                    onSolarThemeAutoChange?.invoke(checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentAmber,
                                    checkedTrackColor = AccentAmber.copy(alpha = 0.35f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.10f)
                                ),
                                modifier = Modifier.testTag("solar_theme_auto_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isSolarThemeAuto) "PREVIEW / OVERRIDE DIURNAL PHASES" else "ACTIVE MANUAL DIURNAL PALETTE",
                            color = TextMuted,
                            style = typo.badgeText.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Phase Selector Chips
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // "Auto Live" pill
                            val isAutoSelected = isSolarThemeAuto
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isAutoSelected) AccentAmber.copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.06f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isAutoSelected) AccentAmber else GlassBorderStart,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        HapticUtils.performClick(view)
                                        onSolarThemeAutoChange?.invoke(true)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isAutoSelected) AccentAmber else TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Auto Live",
                                        color = if (isAutoSelected) AccentAmber else TextSecondary,
                                        style = typo.badgeText.copy(
                                            fontWeight = if (isAutoSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            DiurnalSolarPhase.entries.forEach { phase ->
                                val isSelected = !isSolarThemeAuto && manualDiurnalPhase == phase
                                val samplePalette = SolarThemeScheduler.generatePalette(phase, WeatherConditionType.CLEAR_DAY)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) samplePalette.primaryAccent.copy(alpha = 0.28f)
                                            else Color.White.copy(alpha = 0.06f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) samplePalette.primaryAccent else GlassBorderStart,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            HapticUtils.performClick(view)
                                            onManualDiurnalPhaseChange?.invoke(phase)
                                        }
                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Mini 2-dot color preview
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(samplePalette.primaryAccent, CircleShape)
                                        )
                                        Text(
                                            text = phase.shortLabel,
                                            color = if (isSelected) samplePalette.primaryAccent else TextSecondary,
                                            style = typo.badgeText.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ----------------------------------------------------
                // 2. WORKMANAGER & ROOM CACHE STATUS CARD
                // ----------------------------------------------------
                Text(
                    text = "BACKGROUND WORKMANAGER & CACHE",
                    color = TextMuted,
                    style = typo.sectionHeader
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, AccentCyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(AccentCyan.copy(alpha = 0.18f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudSync,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "15-Min WorkManager Sync",
                                        color = TextPrimary,
                                        style = typo.badgeText.copy(fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    )
                                    Text(
                                        text = "Location: $cityName",
                                        color = TextSecondary,
                                        style = typo.subText.copy(fontSize = 10.5.sp)
                                    )
                                }
                            }

                            // Active Status Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x2E10B981))
                                    .border(1.dp, Color(0x6610B981), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SCHEDULED",
                                        color = Color(0xFF10B981),
                                        style = typo.badgeText.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Room Cache & Last Sync info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Storage,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Room Local Cache: Active",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = typo.subText.copy(fontSize = 11.sp)
                                )
                            }
                            Text(
                                text = "Synced: $syncTimeFormatted",
                                color = TextMuted,
                                style = typo.subText.copy(fontSize = 10.5.sp)
                            )
                        }

                        if (onTriggerSyncNow != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentCyan.copy(alpha = 0.16f))
                                    .border(1.dp, AccentCyan.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        HapticUtils.performClick(view)
                                        onTriggerSyncNow()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sensors,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Execute Immediate Sync Worker",
                                        color = AccentCyan,
                                        style = typo.badgeText.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ----------------------------------------------------
                // 3. BLUR INTENSITY CONTROLS
                // ----------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Opacity,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Blur Intensity",
                            color = TextSecondary,
                            style = typo.subText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentCyan.copy(alpha = 0.18f))
                            .border(1.dp, AccentCyan.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$percentage%",
                            color = AccentCyan,
                            style = typo.badgeText.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = levelDescription,
                    color = AccentAmber,
                    style = typo.subText.copy(fontSize = (11.5f * min(typo.fontScale, 1.25f)).sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Blur Intensity Slider
                Slider(
                    value = blurIntensity,
                    onValueChange = { newValue ->
                        HapticUtils.performTick(view)
                        onIntensityChange(newValue)
                    },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blur_intensity_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0% (Crisp)",
                        color = TextMuted,
                        style = typo.subText.copy(fontSize = 10.5.sp)
                    )
                    Text(
                        text = "50% (Standard)",
                        color = TextMuted,
                        style = typo.subText.copy(fontSize = 10.5.sp)
                    )
                    Text(
                        text = "100% (Frosted)",
                        color = TextMuted,
                        style = typo.subText.copy(fontSize = 10.5.sp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Presets
                Text(
                    text = "QUICK PRESETS",
                    color = TextMuted,
                    style = typo.sectionHeader
                )

                Spacer(modifier = Modifier.height(8.dp))

                val presets = listOf(
                    "Crisp (15%)" to 0.15f,
                    "Light (40%)" to 0.40f,
                    "Balanced (65%)" to 0.65f,
                    "Deep (90%)" to 0.90f
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (label, presetValue) ->
                        val isSelected = (blurIntensity - presetValue).let { it >= -0.05f && it <= 0.05f }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) AccentCyan.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) AccentCyan else GlassBorderStart,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    HapticUtils.performClick(view)
                                    onIntensityChange(presetValue)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) AccentCyan else TextSecondary,
                                style = typo.badgeText.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions: Reset & Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            HapticUtils.performClick(view)
                            onIntensityChange(0.65f) // Default balanced intensity
                            onSolarThemeAutoChange?.invoke(true)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.10f),
                            contentColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = "Reset", style = typo.badgeText)
                        }
                    }

                    Button(
                        onClick = {
                            HapticUtils.performClick(view)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Apply",
                            style = typo.badgeText.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
