package com.skigpxrecorder.ui.session.analysis

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Animated circular progress indicator for performance score (0-100)
 */
@Composable
fun PerformanceScoreCircle(
    score: Float,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 1000),
        label = "score_animation"
    )

    val scoreColor = when {
        score >= 80f -> Color(0xFF4CAF50) // Green
        score >= 60f -> Color(0xFFFFEB3B) // Yellow
        score >= 40f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 20f
            val radius = size.minDimension / 2 - strokeWidth / 2

            // Background circle
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // Progress arc
            val sweepAngle = (animatedScore / 100f) * 360f
            drawArc(
                color = scoreColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.toInt()}",
                fontSize = 48.sp,
                color = scoreColor,
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "Performance",
                fontSize = 14.sp,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
