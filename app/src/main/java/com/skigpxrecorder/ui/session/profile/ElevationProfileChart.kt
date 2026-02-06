package com.skigpxrecorder.ui.session.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.data.model.SkiRun
import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.ui.charts.ChartUtils.mapToScreen
import kotlin.math.roundToInt

/**
 * Large elevation profile chart with optional overlays and zoom
 */
@Composable
fun ElevationProfileChart(
    trackPoints: List<TrackPoint>,
    runs: List<SkiRun>,
    showSpeed: Boolean = false,
    showRunRegions: Boolean = true,
    xAxisMode: com.skigpxrecorder.ui.charts.XAxisMode = com.skigpxrecorder.ui.charts.XAxisMode.DISTANCE,
    modifier: Modifier = Modifier
) {
    var touchedIndex by remember { mutableStateOf<Int?>(null) }
    var zoomStart by remember { mutableStateOf(0f) }
    var zoomEnd by remember { mutableStateOf(1f) }

    val elevationColor = Color(0xFF1976D2) // Material Blue
    val elevationGradient = com.skigpxrecorder.ui.charts.ChartUtils.elevationGradient()
    val speedColor = Color(0xFF4CAF50) // Green

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val chartWidth = size.width - 100f
                    val pointWidth = chartWidth / trackPoints.size.coerceAtLeast(1)
                    val index = ((offset.x - 50f) / pointWidth).toInt()
                        .coerceIn(0, trackPoints.size - 1)
                    touchedIndex = index
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    // Simple zoom: drag adjusts visible range
                    val delta = dragAmount / size.width * 0.5f
                    val newRange = (zoomEnd - zoomStart) - delta
                    if (newRange > 0.1f && newRange <= 1f) {
                        zoomEnd = (zoomEnd - delta / 2).coerceIn(zoomStart + 0.1f, 1f)
                        zoomStart = (zoomStart + delta / 2).coerceIn(0f, zoomEnd - 0.1f)
                    }
                }
            }
    ) {
        if (trackPoints.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 50f
        val chartHeight = height - padding * 2
        val chartWidth = width - padding * 2

        // Calculate visible range
        val startIndex = (trackPoints.size * zoomStart).roundToInt()
        val endIndex = (trackPoints.size * zoomEnd).roundToInt().coerceAtMost(trackPoints.size)
        val visiblePoints = trackPoints.subList(startIndex, endIndex)

        if (visiblePoints.isEmpty()) return@Canvas

        // Calculate ranges for visible points
        val elevations = visiblePoints.map { it.elevation.toFloat() }
        val speeds = visiblePoints.map { it.speed }
        val minElevation = elevations.minOrNull() ?: 0f
        val maxElevation = elevations.maxOrNull() ?: 1f
        val maxSpeed = speeds.maxOrNull() ?: 1f

        // Draw run regions if enabled
        if (showRunRegions) {
            runs.forEach { run ->
                if (run.startIndex in startIndex until endIndex || run.endIndex in startIndex until endIndex) {
                    val runStartIndex = (run.startIndex - startIndex).coerceAtLeast(0)
                    val runEndIndex = (run.endIndex - startIndex).coerceAtMost(visiblePoints.size - 1)

                    if (runStartIndex < visiblePoints.size && runEndIndex >= 0) {
                        val x1 = padding + (runStartIndex.toFloat() / visiblePoints.size) * chartWidth
                        val x2 = padding + (runEndIndex.toFloat() / visiblePoints.size) * chartWidth

                        drawRect(
                            color = Color(0xFF2196F3).copy(alpha = 0.15f),
                            topLeft = Offset(x1, padding),
                            size = androidx.compose.ui.geometry.Size(x2 - x1, chartHeight)
                        )
                    }
                }
            }
        }

        // Draw elevation area (gradient fill)
        val elevationPath = Path().apply {
            visiblePoints.forEachIndexed { index, point ->
                val x = padding + (index.toFloat() / visiblePoints.size) * chartWidth
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
            visiblePoints.forEachIndexed { index, point ->
                val x = padding + (index.toFloat() / visiblePoints.size) * chartWidth
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

        // Draw speed overlay if enabled
        if (showSpeed) {
            val speedPath = Path().apply {
                visiblePoints.forEachIndexed { index, point ->
                    val x = padding + (index.toFloat() / visiblePoints.size) * chartWidth
                    val y = mapToScreen(
                        point.speed,
                        0f,
                        maxSpeed,
                        height - padding,
                        padding
                    )

                    if (index == 0) moveTo(x, y)
                    else lineTo(x, y)
                }
            }

            drawPath(
                path = speedPath,
                color = speedColor.copy(alpha = 0.6f),
                style = Stroke(width = 2f)
            )
        }

        // Draw touched point indicator
        touchedIndex?.let { index ->
            val adjustedIndex = index - startIndex
            if (adjustedIndex in visiblePoints.indices) {
                val x = padding + (adjustedIndex.toFloat() / visiblePoints.size) * chartWidth
                val point = visiblePoints[adjustedIndex]
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
