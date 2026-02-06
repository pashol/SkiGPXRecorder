package com.skigpxrecorder.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Donut chart for displaying percentage breakdowns
 */
@Composable
fun DonutChart(
    data: List<Pair<String, Float>>, // label to percentage
    colors: List<Color>,
    modifier: Modifier = Modifier,
    centerText: String? = null
) {
    require(data.size == colors.size) { "Data and colors lists must have the same size" }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Donut chart
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val chartSize = size.minDimension
                val strokeWidth = chartSize * 0.2f
                val radius = (chartSize - strokeWidth) / 2

                var startAngle = -90f // Start from top

                data.forEachIndexed { index, (_, percentage) ->
                    val sweepAngle = 360f * (percentage / 100f)

                    drawArc(
                        color = colors[index],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        ),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )

                    startAngle += sweepAngle
                }
            }

            // Center text (optional)
            if (centerText != null) {
                Text(
                    text = centerText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Legend
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEachIndexed { index, (label, percentage) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Color indicator
                        Canvas(modifier = Modifier.size(16.dp)) {
                            drawCircle(color = colors[index])
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = String.format("%.1f%%", percentage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
