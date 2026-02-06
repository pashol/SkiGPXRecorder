package com.skigpxrecorder.ui.session.mapview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.data.local.UserPreferences
import com.skigpxrecorder.data.model.SessionStats
import com.skigpxrecorder.util.UnitConverter

/**
 * Bottom card overlay showing key stats on the map
 */
@Composable
fun MapStatsPanel(
    stats: SessionStats,
    unitSystem: UserPreferences.UnitSystem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Distance",
                value = "${UnitConverter.formatDistance(stats.skiDistance, unitSystem)} ${UnitConverter.getDistanceUnit(unitSystem, stats.skiDistance)}"
            )
            StatItem(
                label = "Vertical",
                value = "${UnitConverter.formatElevation(stats.skiVertical, unitSystem)} ${UnitConverter.getElevationUnit(unitSystem)}"
            )
            StatItem(
                label = "Max Speed",
                value = "${UnitConverter.formatSpeed(stats.maxSpeed, unitSystem)} ${UnitConverter.getSpeedUnit(unitSystem)}"
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
