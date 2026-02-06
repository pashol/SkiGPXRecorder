package com.skigpxrecorder.ui.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import kotlin.math.roundToInt

/**
 * Shared utilities for custom chart drawing
 */
object ChartUtils {

    /**
     * Blue vertical gradient for elevation charts
     * From darker blue at top to transparent at bottom
     */
    fun elevationGradient(): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1976D2).copy(alpha = 0.6f),
                Color(0xFF1976D2).copy(alpha = 0.3f),
                Color(0xFF1976D2).copy(alpha = 0.1f),
                Color.Transparent
            )
        )
    }

    /**
     * Speed gradient from green (slow) to yellow to red (fast)
     */
    fun speedGradient(): Brush {
        return Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF4CAF50), // Green
                Color(0xFFFFEB3B), // Yellow
                Color(0xFFF44336)  // Red
            )
        )
    }

    /**
     * Interpolate color along speed gradient
     */
    fun getSpeedColor(normalized: Float): Color {
        return when {
            normalized < 0.33f -> {
                // Green to Yellow
                val t = normalized / 0.33f
                Color(
                    red = 0x4C + ((0xFF - 0x4C) * t).toInt(),
                    green = 0xAF + ((0xEB - 0xAF) * t).toInt(),
                    blue = 0x50 + ((0x3B - 0x50) * t).toInt()
                )
            }
            normalized < 0.66f -> {
                // Yellow to Orange
                val t = (normalized - 0.33f) / 0.33f
                Color(
                    red = 0xFF,
                    green = 0xEB - ((0xEB - 0x98) * t).toInt(),
                    blue = 0x3B - ((0x3B - 0x00) * t).toInt()
                )
            }
            else -> {
                // Orange to Red
                val t = (normalized - 0.66f) / 0.34f
                Color(
                    red = 0xFF - ((0xFF - 0xF4) * t).toInt(),
                    green = 0x98 - ((0x98 - 0x43) * t).toInt(),
                    blue = 0x00 + ((0x36 - 0x00) * t).toInt()
                )
            }
        }
    }

    /**
     * Draw Y-axis with labels
     */
    fun DrawScope.drawYAxis(
        x: Float,
        minValue: Float,
        maxValue: Float,
        divisions: Int = 5,
        labelColor: Color = Color.Gray,
        formatLabel: ((Float) -> String) = { it.roundToInt().toString() }
    ) {
        val height = size.height
        val step = (maxValue - minValue) / divisions

        for (i in 0..divisions) {
            val value = minValue + (step * i)
            val y = height - (height * (value - minValue) / (maxValue - minValue))

            // Draw tick mark
            drawLine(
                color = labelColor,
                start = Offset(x - 10f, y),
                end = Offset(x, y),
                strokeWidth = 2f
            )

            // Draw label
            drawContext.canvas.nativeCanvas.drawText(
                formatLabel(value),
                x - 15f,
                y + 5f,
                Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 26f
                    textAlign = Paint.Align.RIGHT
                }
            )
        }
    }

    /**
     * Draw X-axis with labels
     */
    fun DrawScope.drawXAxis(
        y: Float,
        labels: List<String>,
        labelColor: Color = Color.Gray
    ) {
        val width = size.width
        val step = width / (labels.size - 1).coerceAtLeast(1)

        labels.forEachIndexed { index, label ->
            val x = index * step

            // Draw tick mark
            drawLine(
                color = labelColor,
                start = Offset(x, y),
                end = Offset(x, y + 10f),
                strokeWidth = 2f
            )

            // Draw label
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                y + 30f,
                Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 24f
                    textAlign = Paint.Align.CENTER
                }
            )
        }
    }

    /**
     * Draw grid lines
     */
    fun DrawScope.drawGrid(
        horizontalLines: Int = 5,
        verticalLines: Int = 5,
        color: Color = Color.LightGray.copy(alpha = 0.15f)
    ) {
        val width = size.width
        val height = size.height

        // Horizontal lines
        for (i in 0..horizontalLines) {
            val y = height * i / horizontalLines
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Vertical lines
        for (i in 0..verticalLines) {
            val x = width * i / verticalLines
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
        }
    }

    /**
     * Interpolate speed to color (green = slow, red = fast)
     */
    fun speedToColor(speedKmh: Float, maxSpeed: Float): Color {
        val normalized = (speedKmh / maxSpeed).coerceIn(0f, 1f)
        return when {
            normalized < 0.33f -> Color.Green
            normalized < 0.66f -> Color.Yellow
            else -> Color.Red
        }
    }

    /**
     * Check if a point was touched
     */
    fun isTouched(
        touchX: Float,
        touchY: Float,
        pointX: Float,
        pointY: Float,
        threshold: Float = 50f
    ): Boolean {
        val dx = touchX - pointX
        val dy = touchY - pointY
        return (dx * dx + dy * dy) < (threshold * threshold)
    }

    /**
     * Map data value to screen coordinate
     */
    fun mapToScreen(
        value: Float,
        minValue: Float,
        maxValue: Float,
        screenMin: Float,
        screenMax: Float
    ): Float {
        if (maxValue == minValue) return screenMin
        val normalized = (value - minValue) / (maxValue - minValue)
        return screenMin + normalized * (screenMax - screenMin)
    }

    /**
     * Format distance for chart labels
     */
    fun formatDistance(meters: Float): String {
        return when {
            meters >= 1000 -> String.format("%.1fkm", meters / 1000)
            else -> String.format("%.0fm", meters)
        }
    }

    /**
     * Format time for chart labels (seconds to mm:ss)
     */
    fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", mins, secs)
    }

    /**
     * Find nearest data point index for a given touch X position on the chart
     */
    fun findNearestPointIndex(
        touchX: Float,
        padding: Float,
        chartWidth: Float,
        pointCount: Int
    ): Int {
        if (pointCount <= 0) return 0
        val relativeX = touchX - padding
        val index = (relativeX / chartWidth * pointCount).toInt()
        return index.coerceIn(0, pointCount - 1)
    }

    /**
     * Draw a floating tooltip box with multiple lines of text
     */
    fun DrawScope.drawTooltip(
        x: Float,
        y: Float,
        lines: List<Pair<String, Color>>,
        backgroundColor: Color = Color(0xEE2D2D2D),
        padding: Float = 12f,
        lineHeight: Float = 36f,
        cornerRadius: Float = 12f
    ) {
        val textPaint = Paint().apply {
            textSize = 28f
            isAntiAlias = true
        }

        // Measure text widths to size the box
        val maxTextWidth = lines.maxOf { (text, _) ->
            textPaint.apply { color = Color.White.toArgb() }
            textPaint.measureText(text)
        }

        val boxWidth = maxTextWidth + padding * 2
        val boxHeight = lines.size * lineHeight + padding * 2

        // Position: prefer above the touch point, flip below if near top
        val boxX = (x - boxWidth / 2).coerceIn(0f, size.width - boxWidth)
        val boxY = if (y - boxHeight - 20f > 0f) {
            y - boxHeight - 20f
        } else {
            y + 20f
        }

        // Draw rounded rectangle background
        drawRoundRect(
            color = backgroundColor,
            topLeft = Offset(boxX, boxY),
            size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )

        // Draw text lines
        lines.forEachIndexed { index, (text, textColor) ->
            drawContext.canvas.nativeCanvas.drawText(
                text,
                boxX + padding,
                boxY + padding + (index + 1) * lineHeight - 8f,
                Paint().apply {
                    color = textColor.toArgb()
                    textSize = 28f
                    isAntiAlias = true
                }
            )
        }
    }
}
