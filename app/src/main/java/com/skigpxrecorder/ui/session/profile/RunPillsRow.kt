package com.skigpxrecorder.ui.session.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.data.local.UserPreferences
import com.skigpxrecorder.data.model.SkiRun
import com.skigpxrecorder.util.UnitConverter

/**
 * Horizontally scrollable row of run pill chips
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunPillsRow(
    runs: List<SkiRun>,
    selectedRunIndex: Int?,
    unitSystem: UserPreferences.UnitSystem,
    onRunClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        runs.forEachIndexed { index, run ->
            val isSelected = selectedRunIndex == index
            Card(
                onClick = { onRunClick(index) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Run ${run.runNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color.White else Color.Unspecified
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${UnitConverter.formatElevation(run.verticalDrop, unitSystem)} ${UnitConverter.getElevationUnit(unitSystem)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.Gray
                    )
                    Text(
                        text = "${UnitConverter.formatSpeed(run.maxSpeed, unitSystem)} ${UnitConverter.getSpeedUnit(unitSystem)} max",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.Gray
                    )
                }
            }
        }
    }
}
