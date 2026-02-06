package com.skigpxrecorder.ui.highscore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.skigpxrecorder.ui.charts.DonutChart
import com.skigpxrecorder.ui.components.StatCircle
import com.skigpxrecorder.ui.theme.AppCardDefaults
import com.skigpxrecorder.util.UnitConverter

/**
 * Highscore/Statistics screen showing personal records and lifetime stats
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighscoreScreen(
    navController: NavController,
    viewModel: HighscoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.totalSessions == 0) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No sessions recorded yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start recording your ski sessions to see your statistics!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Personal Records Section
                item {
                    Text(
                        text = "Personal Records",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCircle(
                            value = String.format("%.1f", uiState.maxSpeed),
                            unit = "km/h",
                            label = "Max Speed",
                            progress = (uiState.maxSpeed / 100f).coerceIn(0f, 1f),
                            size = 100.dp,
                            progressColor = MaterialTheme.colorScheme.primary
                        )

                        StatCircle(
                            value = UnitConverter.formatDuration(uiState.totalRideTime / 1000),
                            unit = "",
                            label = "Total Time",
                            progress = 1f, // Always full for time
                            size = 100.dp,
                            progressColor = MaterialTheme.colorScheme.secondary
                        )

                        StatCircle(
                            value = String.format("%.1f", uiState.totalDistanceDownhill / 1000),
                            unit = "km",
                            label = "Distance",
                            progress = 1f, // Always full for cumulative
                            size = 100.dp,
                            progressColor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // Lifetime Stats Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Lifetime Statistics",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = AppCardDefaults.elevation,
                        shape = AppCardDefaults.shape
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LifetimeStat(
                                label = "Total Sessions",
                                value = uiState.totalSessions.toString()
                            )
                            LifetimeStat(
                                label = "Total Ride Time",
                                value = UnitConverter.formatDuration(uiState.totalRideTime / 1000)
                            )
                            LifetimeStat(
                                label = "Max Elevation",
                                value = String.format("%.0f m", uiState.maxElevation)
                            )
                            LifetimeStat(
                                label = "Total Vertical Descent",
                                value = String.format("%.0f m", uiState.totalVerticalDescent)
                            )
                        }
                    }
                }

                // Activity Breakdown Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Activity Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = AppCardDefaults.elevation,
                        shape = AppCardDefaults.shape
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            DonutChart(
                                data = uiState.activityBreakdown.map { it.label to it.percentage },
                                colors = listOf(
                                    Color(0xFF1976D2), // Blue - Skiing
                                    Color(0xFF4CAF50), // Green - Lift
                                    Color(0xFFFFA726), // Orange - Pause
                                    Color(0xFF9E9E9E)  // Grey - Walking
                                )
                            )
                        }
                    }
                }

                // Speed Distribution Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Speed Distribution",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = AppCardDefaults.elevation,
                        shape = AppCardDefaults.shape
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.speedDistribution.forEach { bucket ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = bucket.range,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = bucket.count.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
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
private fun LifetimeStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}
