package com.skigpxrecorder.ui.session.rundetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.data.model.SkiRun

/**
 * Card showing how this run compares to session averages
 */
@Composable
fun RunComparisonCard(
    currentRun: SkiRun,
    allRuns: List<SkiRun>,
    modifier: Modifier = Modifier
) {
    if (allRuns.isEmpty()) return

    val avgSpeed = allRuns.map { it.avgSpeed }.average().toFloat()
    val avgDistance = allRuns.map { it.distance }.average().toFloat()
    val avgVertical = allRuns.map { it.verticalDrop }.average().toFloat()

    val speedDelta = ((currentRun.avgSpeed - avgSpeed) / avgSpeed * 100).toInt()
    val distanceDelta = ((currentRun.distance - avgDistance) / avgDistance * 100).toInt()
    val verticalDelta = ((currentRun.verticalDrop - avgVertical) / avgVertical * 100).toInt()

    val rank = allRuns.sortedByDescending { it.avgSpeed }.indexOf(currentRun) + 1

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Run Comparison",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Rank: #$rank of ${allRuns.size} runs",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ComparisonStat(
                    label = "Speed",
                    delta = speedDelta
                )
                ComparisonStat(
                    label = "Distance",
                    delta = distanceDelta
                )
                ComparisonStat(
                    label = "Vertical",
                    delta = verticalDelta
                )
            }
        }
    }
}

@Composable
private fun ComparisonStat(
    label: String,
    delta: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${if (delta > 0) "+" else ""}$delta%",
            style = MaterialTheme.typography.titleMedium,
            color = when {
                delta > 0 -> Color.Green
                delta < 0 -> Color.Red
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
