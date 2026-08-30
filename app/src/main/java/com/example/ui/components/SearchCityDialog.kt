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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GeocodingResult
import com.example.data.repository.WeatherRepository
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.GlassBorderEnd
import com.example.ui.theme.GlassBorderStart
import com.example.ui.theme.GlassFillEnd
import com.example.ui.theme.GlassFillStart
import com.example.ui.theme.LocalGlassTypography
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.HapticUtils
import kotlin.math.min

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchCityDialog(
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    isSearching: Boolean,
    searchResults: List<GeocodingResult>,
    onCitySelected: (GeocodingResult) -> Unit,
    onDismiss: () -> Unit
) {
    val typo = LocalGlassTypography.current
    val view = LocalView.current

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("search_city_dialog"),
            cornerRadius = 28.dp,
            fillGradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E293B).copy(alpha = 0.96f),
                    Color(0xFF0F172A).copy(alpha = 0.98f)
                )
            ),
            borderGradient = Brush.verticalGradient(
                colors = listOf(AccentCyan.copy(alpha = 0.5f), GlassBorderEnd)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AccentCyan.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, AccentCyan.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationCity,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Search Location",
                            color = TextPrimary,
                            fontSize = (17.5f * min(typo.fontScale, 1.25f)).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            HapticUtils.performTick(view)
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                            .testTag("close_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChanged,
                    placeholder = {
                        Text(
                            text = "Type city name (e.g. Tokyo, London)...",
                            color = TextMuted,
                            style = typo.subText
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = AccentCyan
                        )
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = AccentCyan,
                                strokeWidth = 2.dp
                            )
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color.White.copy(alpha = 0.06f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = AccentCyan
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("city_search_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Results or Popular Cities
                if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
                    Text(
                        text = "SEARCH RESULTS",
                        style = typo.sectionHeader
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(searchResults) { result ->
                            SearchResultItem(result = result, onSelect = {
                                HapticUtils.performClick(view)
                                onCitySelected(result)
                                onDismiss()
                            })
                        }
                    }
                } else {
                    // Popular Quick Cities
                    Text(
                        text = "POPULAR DESTINATIONS",
                        style = typo.sectionHeader
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WeatherRepository.POPULAR_CITIES.forEach { popular ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        HapticUtils.performClick(view)
                                        onCitySelected(popular)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        tint = AccentAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = popular.name,
                                        color = TextPrimary,
                                        style = typo.timeLabel.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    result: GeocodingResult,
    onSelect: () -> Unit
) {
    val typo = LocalGlassTypography.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(AccentAmber.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = result.name,
                    color = TextPrimary,
                    style = typo.timeLabel.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (14 * min(typo.fontScale, 1.25f)).sp
                    )
                )
                val subtitle = listOfNotNull(result.admin1, result.country).joinToString(", ")
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = typo.subText
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Rounded.NearMe,
            contentDescription = "Select",
            tint = AccentCyan,
            modifier = Modifier.size(18.dp)
        )
    }
}
