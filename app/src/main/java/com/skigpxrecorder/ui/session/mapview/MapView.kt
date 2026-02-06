package com.skigpxrecorder.ui.session.mapview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.skigpxrecorder.data.local.UserPreferences
import com.skigpxrecorder.data.model.GPXData
import org.osmdroid.views.MapView as OsmMapView

/**
 * Map view with track overlay and interactive controls
 */
@Composable
fun MapView(
    gpxData: GPXData,
    unitSystem: UserPreferences.UnitSystem,
    modifier: Modifier = Modifier
) {
    var mapType by remember { mutableStateOf(TileSources.MapType.STANDARD) }
    var showSpeed by remember { mutableStateOf(true) }
    var showPistes by remember { mutableStateOf(false) }
    var showMarkers by remember { mutableStateOf(true) }
    var osmMapView by remember { mutableStateOf<OsmMapView?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        // osmdroid Map
        OsmMapWrapper(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { mapView ->
                osmMapView = mapView

                // Set tile source based on map type
                mapView.setTileSource(
                    when (mapType) {
                        TileSources.MapType.STANDARD -> TileSources.OSM
                        TileSources.MapType.TOPOGRAPHIC -> TileSources.TOPO
                        TileSources.MapType.SATELLITE -> TileSources.SATELLITE
                    }
                )

                // Clear existing overlays
                mapView.overlays.clear()

                // Add track polyline
                val trackPolyline = MapOverlays.createTrackPolyline(
                    mapView,
                    gpxData.trackPoints,
                    showSpeed
                )
                mapView.overlays.add(trackPolyline)

                // Add markers if enabled
                if (showMarkers) {
                    // Start/End markers
                    MapOverlays.createStartEndMarkers(mapView, gpxData.trackPoints).forEach {
                        mapView.overlays.add(it)
                    }

                    // Run markers
                    MapOverlays.createRunMarkers(
                        mapView,
                        gpxData.runs,
                        gpxData.trackPoints
                    ).forEach {
                        mapView.overlays.add(it)
                    }

                    // Km markers
                    MapOverlays.createKmMarkers(mapView, gpxData.trackPoints).forEach {
                        mapView.overlays.add(it)
                    }
                }

                // Add piste overlay if enabled
                if (showPistes) {
                    // Note: Piste overlay would be a separate tile overlay
                    // For simplicity, we'll skip this in the MVP
                }

                // Center map on track
                MapOverlays.centerMapOnTrack(mapView, gpxData.trackPoints)

                // Refresh map
                mapView.invalidate()
            }
        )

        // Map controls (top-left)
        MapControls(
            mapType = mapType,
            onMapTypeChange = { newType ->
                mapType = newType
                osmMapView?.setTileSource(
                    when (newType) {
                        TileSources.MapType.STANDARD -> TileSources.OSM
                        TileSources.MapType.TOPOGRAPHIC -> TileSources.TOPO
                        TileSources.MapType.SATELLITE -> TileSources.SATELLITE
                    }
                )
                osmMapView?.invalidate()
            },
            showSpeed = showSpeed,
            onShowSpeedChange = { enabled ->
                showSpeed = enabled
                // Trigger map refresh by recreating overlay
                osmMapView?.let { mapView ->
                    mapView.overlays.clear()
                    val trackPolyline = MapOverlays.createTrackPolyline(
                        mapView,
                        gpxData.trackPoints,
                        showSpeed
                    )
                    mapView.overlays.add(trackPolyline)
                    mapView.invalidate()
                }
            },
            showPistes = showPistes,
            onShowPistesChange = { enabled -> showPistes = enabled },
            showMarkers = showMarkers,
            onShowMarkersChange = { enabled ->
                showMarkers = enabled
                // Trigger map refresh
                osmMapView?.let { mapView ->
                    mapView.overlays.clear()

                    // Re-add track
                    val trackPolyline = MapOverlays.createTrackPolyline(
                        mapView,
                        gpxData.trackPoints,
                        showSpeed
                    )
                    mapView.overlays.add(trackPolyline)

                    // Re-add markers if enabled
                    if (showMarkers) {
                        MapOverlays.createStartEndMarkers(mapView, gpxData.trackPoints).forEach {
                            mapView.overlays.add(it)
                        }
                        MapOverlays.createRunMarkers(
                            mapView,
                            gpxData.runs,
                            gpxData.trackPoints
                        ).forEach {
                            mapView.overlays.add(it)
                        }
                        MapOverlays.createKmMarkers(mapView, gpxData.trackPoints).forEach {
                            mapView.overlays.add(it)
                        }
                    }

                    mapView.invalidate()
                }
            },
            onZoomIn = {
                osmMapView?.controller?.zoomIn()
            },
            onZoomOut = {
                osmMapView?.controller?.zoomOut()
            },
            modifier = Modifier.align(Alignment.TopStart)
        )

        // Stats panel (bottom)
        MapStatsPanel(
            stats = gpxData.stats,
            unitSystem = unitSystem,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Update map when mapType changes
    LaunchedEffect(mapType) {
        osmMapView?.setTileSource(
            when (mapType) {
                TileSources.MapType.STANDARD -> TileSources.OSM
                TileSources.MapType.TOPOGRAPHIC -> TileSources.TOPO
                TileSources.MapType.SATELLITE -> TileSources.SATELLITE
            }
        )
        osmMapView?.invalidate()
    }
}
