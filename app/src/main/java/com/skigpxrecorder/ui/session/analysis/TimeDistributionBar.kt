package com.skigpxrecorder.ui.session.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.util.UnitConverter

/**
 * Horizontal stacked bar showing time distribution
 */
@Composable
fun TimeDistributionBar(
    movingTime: Long,
    stationaryTime: Long,
    ascendingTime: Long,
    descendingTime: Long,
    modifier: Modifier = Modifier
) {
    val total = movingTime + stationaryTime + ascendingTime + descendingTime
    if (total == 0L) return

    val movingPercent = movingTime.toFloat() / total
    val stationaryPercent = stationaryTime.toFloat() / total
    val ascendingPercent = ascendingTime.toFloat() / total
    val descendingPercent = descendingTime.toFloat() / total

    val descendingColor = Color(0xFF2196F3) // Blue
    val ascendingColor = Color(0xFFFF9800) // Orange
    val movingColor = Color(0xFF4CAF50) // Green
    val stationaryColor = Color(0xFF9E9E9E) // Gray

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Time Distribution",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Stacked bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                if (descendingPercent > 0) {
                    Box(
                        modifier = Modifier
                            .weight(descendingPercent)
                            .height(32.dp)
                            .background(descendingColor, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                    )
                }
                if (ascendingPercent > 0) {
                    Box(
                        modifier = Modifier
                            .weight(ascendingPercent)
                            .height(32.dp)
                            .background(ascendingColor)
                    )
                }
                if (movingPercent > 0) {
                    Box(
                        modifier = Modifier
                            .weight(movingPercent)
                            .height(32.dp)
                            .background(movingColor)
                    )
                }
                if (stationaryPercent > 0) {
                    Box(
                        modifier = Modifier
                            .weight(stationaryPercent)
                            .height(32.dp)
                            .background(stationaryColor, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Column {
                LegendItem("Descending", UnitConverter.formatDuration(descendingTime), descendingColor)
                LegendItem("Ascending", UnitConverter.formatDuration(ascendingTime), ascendingColor)
                LegendItem("Moving", UnitConverter.formatDuration(movingTime), movingColor)
                LegendItem("Stationary", UnitConverter.formatDuration(stationaryTime), stationaryColor)
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, duration: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = duration,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}
