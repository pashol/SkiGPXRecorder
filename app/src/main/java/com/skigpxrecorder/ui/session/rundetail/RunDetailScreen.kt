package com.skigpxrecorder.ui.session.rundetail

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.skigpxrecorder.data.local.UserPreferences
import com.skigpxrecorder.ui.charts.ElevationSpeedChart
import com.skigpxrecorder.ui.charts.SpeedHistogram
import com.skigpxrecorder.ui.session.trackview.StatCard
import com.skigpxrecorder.util.UnitConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detailed view of a single ski run
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailScreen(
    sessionId: String,
    runNumber: Int,
    navController: NavController,
    viewModel: RunDetailViewModel = hiltViewModel()
) {
    val run by viewModel.run.collectAsState()
    val runPoints by viewModel.runPoints.collectAsState()
    val allRuns by viewModel.allRuns.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(sessionId, runNumber) {
        viewModel.loadRun(sessionId, runNumber)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run #$runNumber") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                run == null -> {
                    Text(
                        text = "Run not found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    RunDetailContent(
                        run = run!!,
                        runPoints = runPoints,
                        allRuns = allRuns,
                        unitSystem = UserPreferences.UnitSystem.METRIC
                    )
                }
            }
        }
    }
}

@Composable
private fun RunDetailContent(
    run: com.skigpxrecorder.data.model.SkiRun,
    runPoints: List<com.skigpxrecorder.data.model.TrackPoint>,
    allRuns: List<com.skigpxrecorder.data.model.SkiRun>,
    unitSystem: UserPreferences.UnitSystem
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time info
        item {
            Column {
                Text(
                    text = formatTime(run.startTime),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Duration: ${UnitConverter.formatDuration((run.endTime - run.startTime) / 1000)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Stats grid
        item {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Max Speed",
                    value = String.format("%.1f", run.maxSpeed),
                    unit = UnitConverter.getSpeedUnit(unitSystem),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Avg Speed",
                    value = String.format("%.1f", run.avgSpeed),
                    unit = UnitConverter.getSpeedUnit(unitSystem),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Distance",
                    value = String.format("%.2f", run.distance / 1000),
                    unit = "km",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Vertical Drop",
                    value = String.format("%.0f", run.verticalDrop),
                    unit = UnitConverter.getElevationUnit(unitSystem),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Start Elevation",
                    value = String.format("%.0f", run.startElevation),
                    unit = UnitConverter.getElevationUnit(unitSystem),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "End Elevation",
                    value = String.format("%.0f", run.endElevation),
                    unit = UnitConverter.getElevationUnit(unitSystem),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Avg Slope",
                    value = UnitConverter.formatSlope(run.avgSlope),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Points",
                    value = run.pointCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Elevation & Speed Chart
        if (runPoints.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Elevation & Speed Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                ElevationSpeedChart(
                    trackPoints = runPoints,
                    showSpeed = true
                )
            }

            // Speed Histogram
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Speed Distribution",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                val speedBuckets = calculateSpeedBuckets(runPoints)
                SpeedHistogram(speedBuckets = speedBuckets)
            }
        }

        // Run Comparison
        if (allRuns.size > 1) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                RunComparisonCard(
                    currentRun = run,
                    allRuns = allRuns
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun calculateSpeedBuckets(points: List<com.skigpxrecorder.data.model.TrackPoint>): List<Int> {
    val buckets = IntArray(6)
    points.forEach { point ->
        val bucket = when {
            point.speed < 10f -> 0
            point.speed < 20f -> 1
            point.speed < 30f -> 2
            point.speed < 40f -> 3
            point.speed < 50f -> 4
            else -> 5
        }
        buckets[bucket]++
    }
    return buckets.toList()
}
