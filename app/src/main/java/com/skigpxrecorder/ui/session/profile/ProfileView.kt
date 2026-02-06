package com.skigpxrecorder.ui.session.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.data.local.UserPreferences
import com.skigpxrecorder.data.model.GPXData
import com.skigpxrecorder.ui.charts.XAxisMode
import com.skigpxrecorder.ui.session.trackview.StatCard
import com.skigpxrecorder.util.UnitConverter

/**
 * Profile view showing elevation profile with overlays and controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileView(
    gpxData: GPXData,
    unitSystem: UserPreferences.UnitSystem,
    modifier: Modifier = Modifier
) {
    var showSpeed by remember { mutableStateOf(false) }
    var showRunRegions by remember { mutableStateOf(true) }
    var xAxisMode by remember { mutableStateOf(XAxisMode.DISTANCE) }
    var selectedRunIndex by remember { mutableStateOf<Int?>(null) }

    val stats = gpxData.stats
    val trackPoints = gpxData.trackPoints
    val runs = gpxData.runs

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toggle controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Display Options",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = showSpeed,
                            onClick = { showSpeed = !showSpeed },
                            label = { Text("Speed Overlay") }
                        )
                        FilterChip(
                            selected = showRunRegions,
                            onClick = { showRunRegions = !showRunRegions },
                            label = { Text("Run Regions") }
                        )
                        FilterChip(
                            selected = xAxisMode == XAxisMode.TIME,
                            onClick = {
                                xAxisMode = if (xAxisMode == XAxisMode.DISTANCE) {
                                    XAxisMode.TIME
                                } else {
                                    XAxisMode.DISTANCE
                                }
                            },
                            label = { Text(if (xAxisMode == XAxisMode.DISTANCE) "Distance" else "Time") }
                        )
                    }
                }
            }
        }

        // Elevation profile chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Elevation Profile",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ElevationProfileChart(
                        trackPoints = trackPoints,
                        runs = runs,
                        showSpeed = showSpeed,
                        showRunRegions = showRunRegions,
                        xAxisMode = xAxisMode
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LegendItem("Elevation", Color(0xFF9C27B0))
                        if (showSpeed) {
                            LegendItem("Speed", Color.Blue)
                        }
                        if (showRunRegions) {
                            LegendItem("Run Region", Color(0xFF2196F3).copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        // Stats summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Elevation Stats",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Max Altitude",
                            value = UnitConverter.formatElevation(stats.maxAltitude, unitSystem),
                            unit = UnitConverter.getElevationUnit(unitSystem),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Min Altitude",
                            value = UnitConverter.formatElevation(stats.minAltitude, unitSystem),
                            unit = UnitConverter.getElevationUnit(unitSystem),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Total Ascent",
                            value = UnitConverter.formatElevation(stats.totalAscent, unitSystem),
                            unit = UnitConverter.getElevationUnit(unitSystem),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Total Descent",
                            value = UnitConverter.formatElevation(stats.totalDescent, unitSystem),
                            unit = UnitConverter.getElevationUnit(unitSystem),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Run pills
        if (runs.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Ski Runs",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        RunPillsRow(
                            runs = runs,
                            selectedRunIndex = selectedRunIndex,
                            unitSystem = unitSystem,
                            onRunClick = { index ->
                                selectedRunIndex = if (selectedRunIndex == index) null else index
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .height(3.dp)
                .fillMaxWidth(0.1f)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
