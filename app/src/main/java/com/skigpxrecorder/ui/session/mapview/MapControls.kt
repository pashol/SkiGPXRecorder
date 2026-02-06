package com.skigpxrecorder.ui.session.mapview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Floating controls for map view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapControls(
    mapType: TileSources.MapType,
    onMapTypeChange: (TileSources.MapType) -> Unit,
    showSpeed: Boolean,
    onShowSpeedChange: (Boolean) -> Unit,
    showPistes: Boolean,
    onShowPistesChange: (Boolean) -> Unit,
    showMarkers: Boolean,
    onShowMarkersChange: (Boolean) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Map type selector
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = mapType == TileSources.MapType.STANDARD,
                onClick = { onMapTypeChange(TileSources.MapType.STANDARD) },
                label = { Text("Map") }
            )
            FilterChip(
                selected = mapType == TileSources.MapType.TOPOGRAPHIC,
                onClick = { onMapTypeChange(TileSources.MapType.TOPOGRAPHIC) },
                label = { Text("Topo") }
            )
            FilterChip(
                selected = mapType == TileSources.MapType.SATELLITE,
                onClick = { onMapTypeChange(TileSources.MapType.SATELLITE) },
                label = { Text("Satellite") }
            )
        }

        // Overlay toggles
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = showSpeed,
                onClick = { onShowSpeedChange(!showSpeed) },
                label = { Text("Speed") }
            )
            FilterChip(
                selected = showPistes,
                onClick = { onShowPistesChange(!showPistes) },
                label = { Text("Pistes") }
            )
            FilterChip(
                selected = showMarkers,
                onClick = { onShowMarkersChange(!showMarkers) },
                label = { Text("Markers") }
            )
        }

        // Zoom controls
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FloatingActionButton(
                onClick = onZoomIn,
                modifier = Modifier.padding(4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }
            FloatingActionButton(
                onClick = onZoomOut,
                modifier = Modifier.padding(4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "−",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}
