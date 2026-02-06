package com.skigpxrecorder.ui.session.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import android.graphics.Paint

/**
 * 5-zone heart rate chart with colored horizontal bars
 */
@Composable
fun HeartRateZones(
    avgHeartRate: Float,
    maxHeartRate: Float,
    modifier: Modifier = Modifier
) {
    val zones = listOf(
        HRZone("Zone 1", 0.5f, 0.6f, Color(0xFF9E9E9E)), // Gray
        HRZone("Zone 2", 0.6f, 0.7f, Color(0xFF2196F3)), // Blue
        HRZone("Zone 3", 0.7f, 0.8f, Color(0xFF4CAF50)), // Green
        HRZone("Zone 4", 0.8f, 0.9f, Color(0xFFFF9800)), // Orange
        HRZone("Zone 5", 0.9f, 1.0f, Color(0xFFF44336))  // Red
    )

    // Estimate max HR (220 - age), default to 180 if not available
    val estimatedMaxHR = if (maxHeartRate > 0) maxHeartRate else 180f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Heart Rate Zones",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Avg: ${avgHeartRate.toInt()} bpm  |  Max: ${maxHeartRate.toInt()} bpm",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val width = size.width
                val height = size.height
                val barHeight = height / zones.size
                val padding = 40f

                zones.forEachIndexed { index, zone ->
                    val y = index * barHeight
                    val minHR = estimatedMaxHR * zone.minPercent
                    val maxHR = estimatedMaxHR * zone.maxPercent

                    // Calculate percentage of time in this zone (simplified - just show if avg HR is in zone)
                    val percentage = if (avgHeartRate >= minHR && avgHeartRate <= maxHR) 0.7f else 0.2f
                    val barWidth = (width - padding * 2) * percentage

                    // Draw bar
                    drawRoundRect(
                        color = zone.color.copy(alpha = 0.8f),
                        topLeft = Offset(padding, y + 10f),
                        size = Size(barWidth, barHeight - 20f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // Draw zone label
                    drawContext.canvas.nativeCanvas.drawText(
                        zone.name,
                        padding + barWidth + 10f,
                        y + barHeight / 2 + 5f,
                        Paint().apply {
                            color = zone.color.toArgb()
                            textSize = 28f
                            textAlign = Paint.Align.LEFT
                        }
                    )

                    // Draw HR range
                    drawContext.canvas.nativeCanvas.drawText(
                        "${minHR.toInt()}-${maxHR.toInt()} bpm",
                        10f,
                        y + barHeight / 2 + 5f,
                        Paint().apply {
                            color = Color.Gray.toArgb()
                            textSize = 24f
                            textAlign = Paint.Align.LEFT
                        }
                    )
                }
            }
        }
    }
}

private data class HRZone(
    val name: String,
    val minPercent: Float,
    val maxPercent: Float,
    val color: Color
)
