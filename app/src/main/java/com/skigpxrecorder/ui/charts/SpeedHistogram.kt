package com.skigpxrecorder.ui.charts

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * Speed histogram showing distribution across speed buckets
 */
@Composable
fun SpeedHistogram(
    speedBuckets: List<Int>, // Count of points in each bucket
    bucketLabels: List<String> = listOf("0-10", "10-20", "20-30", "30-40", "40-50", "50+"),
    modifier: Modifier = Modifier
) {
    // Use gradient colors for bars (green to red)
    val speedGradient = ChartUtils.speedGradient()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val width = size.width
        val height = size.height
        val padding = 40f
        val chartHeight = height - padding * 2
        val chartWidth = width - padding * 2

        val maxCount = speedBuckets.maxOrNull()?.toFloat() ?: 1f
        val barWidth = chartWidth / speedBuckets.size
        val barSpacing = barWidth * 0.1f
        val actualBarWidth = barWidth - barSpacing

        speedBuckets.forEachIndexed { index, count ->
            val barHeight = if (maxCount > 0) {
                (count / maxCount) * chartHeight
            } else 0f

            val x = padding + index * barWidth
            val y = height - padding - barHeight

            // Get color based on bucket index (normalized across all buckets)
            val normalized = index.toFloat() / speedBuckets.size.coerceAtLeast(1)
            val barColor = ChartUtils.getSpeedColor(normalized)

            // Draw bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(actualBarWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Draw label
            drawContext.canvas.nativeCanvas.drawText(
                bucketLabels.getOrNull(index) ?: "",
                x + actualBarWidth / 2,
                height - 10f,
                Paint().apply {
                    color = Color.Gray.toArgb()
                    textSize = 24f
                    textAlign = Paint.Align.CENTER
                }
            )

            // Draw count if significant
            if (count > 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    count.toString(),
                    x + actualBarWidth / 2,
                    y - 10f,
                    Paint().apply {
                        color = barColor.toArgb()
                        textSize = 28f
                        textAlign = Paint.Align.CENTER
                    }
                )
            }
        }
    }
}
