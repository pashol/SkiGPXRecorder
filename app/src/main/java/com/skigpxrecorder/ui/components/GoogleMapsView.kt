package com.skigpxrecorder.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.skigpxrecorder.data.model.TrackPoint

@Composable
fun GoogleMapsView(
    trackPoints: List<TrackPoint>,
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        // Default to a central European ski area
        position = CameraPosition.fromLatLngZoom(LatLng(47.0, 11.0), 6f)
    }

    val mapProperties = remember(hasLocationPermission) {
        MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            mapType = MapType.TERRAIN
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = true,
            compassEnabled = true
        )
    }

    // Follow latest track point
    LaunchedEffect(trackPoints.size) {
        if (trackPoints.isEmpty()) return@LaunchedEffect
        val lastPoint = trackPoints.last()
        val latLng = LatLng(lastPoint.latitude, lastPoint.longitude)

        if (trackPoints.size <= 2) {
            // Initial zoom when recording starts
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(latLng, 16f)
                )
            )
        } else {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(latLng),
                durationMs = 500
            )
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings
    ) {
        if (trackPoints.size >= 2) {
            val polylinePoints = trackPoints.map { LatLng(it.latitude, it.longitude) }
            Polyline(
                points = polylinePoints,
                color = Color(0xFF1976D2),
                width = 8f
            )
        }
    }
}
