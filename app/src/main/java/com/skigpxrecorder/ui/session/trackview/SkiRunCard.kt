package com.skigpxrecorder.ui.session.trackview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.data.local.UserPreferences
import com.skigpxrecorder.data.model.SkiRun
import com.skigpxrecorder.ui.theme.AppCardDefaults
import com.skigpxrecorder.util.UnitConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Card displaying ski run summary information
 */
@Composable
fun SkiRunCard(
    run: SkiRun,
    unitSystem: UserPreferences.UnitSystem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = AppCardDefaults.elevation,
        shape = AppCardDefaults.shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Run number and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Run #${run.runNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatTime(run.startTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Duration and distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RunStat(
                    label = "Duration",
                    value = UnitConverter.formatDuration((run.endTime - run.startTime) / 1000),
                    modifier = Modifier.weight(1f)
                )
                RunStat(
                    label = "Distance",
                    value = UnitConverter.formatDistance(run.distance, unitSystem),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vertical and speeds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RunStat(
                    label = "Vertical",
                    value = UnitConverter.formatElevation(run.verticalDrop, unitSystem),
                    modifier = Modifier.weight(1f)
                )
                RunStat(
                    label = "Max Speed",
                    value = UnitConverter.formatSpeed(run.maxSpeed, unitSystem),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Average speed and slope
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RunStat(
                    label = "Avg Speed",
                    value = UnitConverter.formatSpeed(run.avgSpeed, unitSystem),
                    modifier = Modifier.weight(1f)
                )
                RunStat(
                    label = "Slope",
                    value = UnitConverter.formatSlope(run.avgSlope),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RunStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
