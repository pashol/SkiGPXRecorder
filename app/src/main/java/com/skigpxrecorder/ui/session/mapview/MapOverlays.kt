package com.skigpxrecorder.ui.session.mapview

import android.graphics.Color
import android.graphics.Paint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.data.model.SkiRun
import com.skigpxrecorder.ui.charts.ChartUtils

/**
 * Creates map overlays for tracks, markers, and regions
 */
object MapOverlays {

    /**
     * Create a polyline overlay from track points
     */
    fun createTrackPolyline(
        mapView: MapView,
        trackPoints: List<TrackPoint>,
        showSpeed: Boolean = false
    ): Polyline {
        val polyline = Polyline(mapView)
        val geoPoints = trackPoints.map { GeoPoint(it.latitude, it.longitude) }
        polyline.setPoints(geoPoints)

        if (showSpeed && trackPoints.isNotEmpty()) {
            // Color-code by speed (simplified - just use average color)
            val maxSpeed = trackPoints.maxOfOrNull { it.speed } ?: 1f
            val avgSpeed = trackPoints.map { it.speed }.average().toFloat()
            val speedColor = ChartUtils.speedToColor(avgSpeed, maxSpeed)
            polyline.outlinePaint.color = android.graphics.Color.argb(
                200,
                (speedColor.red * 255).toInt(),
                (speedColor.green * 255).toInt(),
                (speedColor.blue * 255).toInt()
            )
        } else {
            polyline.outlinePaint.color = Color.BLUE
        }

        polyline.outlinePaint.strokeWidth = 8f
        polyline.outlinePaint.strokeCap = Paint.Cap.ROUND
        polyline.outlinePaint.strokeJoin = Paint.Join.ROUND

        return polyline
    }

    /**
     * Create start and end markers
     */
    fun createStartEndMarkers(
        mapView: MapView,
        trackPoints: List<TrackPoint>
    ): List<Marker> {
        if (trackPoints.isEmpty()) return emptyList()

        val markers = mutableListOf<Marker>()

        // Start marker
        val startPoint = trackPoints.first()
        val startMarker = Marker(mapView).apply {
            position = GeoPoint(startPoint.latitude, startPoint.longitude)
            title = "Start"
            snippet = "Session Start"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        markers.add(startMarker)

        // End marker
        val endPoint = trackPoints.last()
        val endMarker = Marker(mapView).apply {
            position = GeoPoint(endPoint.latitude, endPoint.longitude)
            title = "End"
            snippet = "Session End"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        markers.add(endMarker)

        return markers
    }

    /**
     * Create run start markers
     */
    fun createRunMarkers(
        mapView: MapView,
        runs: List<SkiRun>,
        trackPoints: List<TrackPoint>
    ): List<Marker> {
        val markers = mutableListOf<Marker>()

        runs.forEach { run ->
            if (run.startIndex in trackPoints.indices) {
                val startPoint = trackPoints[run.startIndex]
                val marker = Marker(mapView).apply {
                    position = GeoPoint(startPoint.latitude, startPoint.longitude)
                    title = "Run ${run.runNumber}"
                    snippet = "Vertical: ${run.verticalDrop.toInt()}m\nMax Speed: ${run.maxSpeed.toInt()} km/h"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                markers.add(marker)
            }
        }

        return markers
    }

    /**
     * Create kilometer markers along the track
     */
    fun createKmMarkers(
        mapView: MapView,
        trackPoints: List<TrackPoint>
    ): List<Marker> {
        val markers = mutableListOf<Marker>()
        var totalDistance = 0f
        var nextKm = 1000f // First marker at 1km

        for (i in 1 until trackPoints.size) {
            val prevPoint = trackPoints[i - 1]
            val currentPoint = trackPoints[i]

            // Calculate distance between consecutive points
            val segmentDistance = com.skigpxrecorder.domain.StatsCalculator.calculateDistance(
                prevPoint,
                currentPoint
            ).toFloat()
            totalDistance += segmentDistance

            // Add marker at each kilometer
            if (totalDistance >= nextKm) {
                val marker = Marker(mapView).apply {
                    position = GeoPoint(currentPoint.latitude, currentPoint.longitude)
                    title = "${(nextKm / 1000).toInt()} km"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                markers.add(marker)
                nextKm += 1000f
            }
        }

        return markers
    }

    /**
     * Center map on track bounds
     */
    fun centerMapOnTrack(mapView: MapView, trackPoints: List<TrackPoint>) {
        if (trackPoints.isEmpty()) return

        val latitudes = trackPoints.map { it.latitude }
        val longitudes = trackPoints.map { it.longitude }

        val minLat = latitudes.minOrNull() ?: return
        val maxLat = latitudes.maxOrNull() ?: return
        val minLon = longitudes.minOrNull() ?: return
        val maxLon = longitudes.maxOrNull() ?: return

        val centerLat = (minLat + maxLat) / 2
        val centerLon = (minLon + maxLon) / 2

        mapView.controller.setCenter(GeoPoint(centerLat, centerLon))

        // Calculate appropriate zoom level based on bounds
        val latSpan = maxLat - minLat
        val lonSpan = maxLon - minLon
        val maxSpan = maxOf(latSpan, lonSpan)

        val zoom = when {
            maxSpan > 0.1 -> 10.0
            maxSpan > 0.05 -> 12.0
            maxSpan > 0.01 -> 14.0
            maxSpan > 0.005 -> 15.0
            else -> 16.0
        }

        mapView.controller.setZoom(zoom)
    }
}
