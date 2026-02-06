package com.skigpxrecorder.ui.session.trackview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.data.local.UserPreferences
import com.skigpxrecorder.data.model.GPXData
import com.skigpxrecorder.util.UnitConverter

/**
 * Track overview dashboard showing session statistics and ski runs
 */
@Composable
fun TrackView(
    gpxData: GPXData,
    unitSystem: UserPreferences.UnitSystem = UserPreferences.UnitSystem.METRIC,
    onRunClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recording indicator (if live)
        if (gpxData.metadata.isLive) {
            item {
                val duration = (System.currentTimeMillis() - gpxData.metadata.startTime) / 1000
                val lastPoint = gpxData.trackPoints.lastOrNull()
                RecordingIndicator(
                    elapsedTime = duration,
                    gpsAccuracy = lastPoint?.accuracy ?: 0f,
                    pointCount = gpxData.trackPoints.size
                )
            }
        }

        // Stats section title
        item {
            Text(
                text = "Session Statistics",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Speed stats (2 columns)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Max Speed",
                    value = String.format("%.1f", gpxData.stats.maxSpeed),
                    unit = UnitConverter.getSpeedUnit(unitSystem),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Avg Speed",
                    value = String.format("%.1f", gpxData.stats.avgSpeed),
                    unit = UnitConverter.getSpeedUnit(unitSystem),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Distance stats (2 columns)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Ski Distance",
                    value = String.format("%.2f", gpxData.stats.skiDistance / 1000),
                    unit = "km",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Total Distance",
                    value = String.format("%.2f", gpxData.stats.totalDistance / 1000),
                    unit = "km",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Vertical stats (2 columns)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Ski Vertical",
                    value = String.format("%.0f", gpxData.stats.skiVertical),
                    unit = UnitConverter.getElevationUnit(unitSystem),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Total Vertical",
                    value = String.format("%.0f", gpxData.stats.totalDescent),
                    unit = UnitConverter.getElevationUnit(unitSystem),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Altitude range (2 columns)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Min Altitude",
                    value = String.format("%.0f", gpxData.stats.minAltitude),
                    unit = UnitConverter.getElevationUnit(unitSystem),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Max Altitude",
                    value = String.format("%.0f", gpxData.stats.maxAltitude),
                    unit = UnitConverter.getElevationUnit(unitSystem),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Run count and duration (2 columns)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Runs",
                    value = gpxData.runs.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Duration",
                    value = UnitConverter.formatDuration(gpxData.stats.totalDuration),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Ski Runs section
        if (gpxData.runs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ski Runs",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(gpxData.runs) { run ->
                SkiRunCard(
                    run = run,
                    unitSystem = unitSystem,
                    onClick = { onRunClick(run.runNumber - 1) }
                )
            }
        }
    }
}
