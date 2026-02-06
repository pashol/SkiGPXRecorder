package com.skigpxrecorder.ui.session.trackview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.ui.theme.AppCardDefaults

/**
 * Reusable stat card for displaying metrics
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    unit: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = AppCardDefaults.elevation,
        shape = AppCardDefaults.shape
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (unit.isNotEmpty()) "$value $unit" else value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
