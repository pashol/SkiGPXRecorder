package com.skigpxrecorder.ui.charts

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.ui.charts.ChartUtils.drawGrid
import com.skigpxrecorder.ui.charts.ChartUtils.drawYAxis
import com.skigpxrecorder.ui.charts.ChartUtils.mapToScreen

/**
 * Dual-axis chart showing elevation profile and speed overlay
 */
@Composable
fun ElevationSpeedChart(
    trackPoints: List<TrackPoint>,
    showSpeed: Boolean = true,
    xAxisMode: XAxisMode = XAxisMode.DISTANCE,
    modifier: Modifier = Modifier
) {
    var touchedIndex by remember { mutableStateOf<Int?>(null) }

    val elevationColor = Color(0xFF1976D2) // Material Blue
    val elevationGradient = ChartUtils.elevationGradient()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Find closest point to touch
                    val chartWidth = size.width - 100f
                    val pointWidth = chartWidth / trackPoints.size.coerceAtLeast(1)
                    val index = ((offset.x - 50f) / pointWidth).toInt()
                        .coerceIn(0, trackPoints.size - 1)
                    touchedIndex = index
                }
            }
    ) {
        if (trackPoints.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 50f
        val chartHeight = height - padding * 2
        val chartWidth = width - padding * 2

        // Calculate ranges
        val elevations = trackPoints.map { it.elevation.toFloat() }
        val speeds = trackPoints.map { it.speed }
        val minElevation = elevations.minOrNull() ?: 0f
        val maxElevation = elevations.maxOrNull() ?: 1f
        val maxSpeed = speeds.maxOrNull() ?: 1f

        // Draw grid
        drawGrid(horizontalLines = 5, verticalLines = 5)

        // Draw elevation area (gradient fill)
        val elevationPath = Path().apply {
            trackPoints.forEachIndexed { index, point ->
                val x = padding + (index.toFloat() / trackPoints.size) * chartWidth
                val y = mapToScreen(
                    point.elevation.toFloat(),
                    minElevation,
                    maxElevation,
                    height - padding,
                    padding
                )

                if (index == 0) {
                    moveTo(x, height - padding)
                    lineTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
            // Close path to create filled area
            lineTo(padding + chartWidth, height - padding)
            close()
        }

        drawPath(
            path = elevationPath,
            brush = elevationGradient,
            style = Fill
        )

        // Draw elevation line
        val elevationLinePath = Path().apply {
            trackPoints.forEachIndexed { index, point ->
                val x = padding + (index.toFloat() / trackPoints.size) * chartWidth
                val y = mapToScreen(
                    point.elevation.toFloat(),
                    minElevation,
                    maxElevation,
                    height - padding,
                    padding
                )

                if (index == 0) moveTo(x, y)
                else lineTo(x, y)
            }
        }

        drawPath(
            path = elevationLinePath,
            color = elevationColor,
            style = Stroke(width = 3f)
        )

        // Draw speed line if enabled (with gradient coloring)
        if (showSpeed) {
            trackPoints.forEachIndexed { index, point ->
                if (index > 0) {
                    val prevPoint = trackPoints[index - 1]
                    val x1 = padding + ((index - 1).toFloat() / trackPoints.size) * chartWidth
                    val y1 = mapToScreen(
                        prevPoint.speed,
                        0f,
                        maxSpeed,
                        height - padding,
                        padding
                    )
                    val x2 = padding + (index.toFloat() / trackPoints.size) * chartWidth
                    val y2 = mapToScreen(
                        point.speed,
                        0f,
                        maxSpeed,
                        height - padding,
                        padding
                    )

                    // Color based on current speed
                    val normalized = (point.speed / maxSpeed).coerceIn(0f, 1f)
                    val speedColor = ChartUtils.getSpeedColor(normalized)

                    drawLine(
                        color = speedColor,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 3f
                    )
                }
            }
        }

        // Draw Y-axes
        drawContext.canvas.nativeCanvas.apply {
            // Left Y-axis (elevation)
            ChartUtils.run {
                drawYAxis(
                    x = padding,
                    minValue = minElevation,
                    maxValue = maxElevation,
                    divisions = 5,
                    labelColor = elevationColor,
                    formatLabel = { "${it.toInt()}m" }
                )
            }

            // Right Y-axis (speed) if showing speed
            if (showSpeed) {
                ChartUtils.run {
                    drawYAxis(
                        x = width - padding,
                        minValue = 0f,
                        maxValue = maxSpeed,
                        divisions = 5,
                        labelColor = Color(0xFF4CAF50), // Green for speed axis
                        formatLabel = { "${it.toInt()} km/h" }
                    )
                }
            }
        }

        // Draw touched point indicator
        touchedIndex?.let { index ->
            if (index in trackPoints.indices) {
                val x = padding + (index.toFloat() / trackPoints.size) * chartWidth
                val point = trackPoints[index]
                val y = mapToScreen(
                    point.elevation.toFloat(),
                    minElevation,
                    maxElevation,
                    height - padding,
                    padding
                )

                // Draw vertical line
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(x, padding),
                    end = Offset(x, height - padding),
                    strokeWidth = 2f
                )

                // Draw tooltip circle
                drawCircle(
                    color = elevationColor,
                    radius = 8f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

enum class XAxisMode {
    DISTANCE,
    TIME
}
