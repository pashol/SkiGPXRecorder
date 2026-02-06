package com.skigpxrecorder.ui.session.rundetail

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.skigpxrecorder.data.model.TrackPoint
import com.skigpxrecorder.ui.charts.ChartUtils
import com.skigpxrecorder.ui.session.mapview.TileSources
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.tileprovider.MapTileProviderBasic

/**
 * Interactive map view for a single ski run with speed-colored polyline,
 * start/end markers, synced position marker, and collapsible layer controls
 */
@Composable
fun RunMapView(
    trackPoints: List<TrackPoint>,
    runNumber: Int,
    highlightedPointIndex: Int?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapType by remember { mutableStateOf(TileSources.MapType.STANDARD) }
    var showPistes by remember { mutableStateOf(true) }
    var showLayersMenu by remember { mutableStateOf(false) }
    var osmMapView by remember { mutableStateOf<MapView?>(null) }
    var positionMarker by remember { mutableStateOf<Marker?>(null) }

    // Initialize osmdroid configuration
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = context.getExternalFilesDir(null)
            osmdroidTileCache = context.getExternalFilesDir("osmdroid")?.apply {
                if (!exists()) mkdirs()
            }
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(46.0, 11.0))
        }
    }

    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Update highlighted position marker when chart selection changes
    LaunchedEffect(highlightedPointIndex) {
        val map = osmMapView ?: return@LaunchedEffect
        // Remove existing position marker
        positionMarker?.let { map.overlays.remove(it) }
        positionMarker = null

        if (highlightedPointIndex != null && highlightedPointIndex in trackPoints.indices) {
            val point = trackPoints[highlightedPointIndex]
            val marker = Marker(map).apply {
                position = GeoPoint(point.latitude, point.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createCircleDrawable(0xFF2196F3.toInt(), 24)
                title = null
            }
            map.overlays.add(marker)
            positionMarker = marker
        }
        map.invalidate()
    }

    // Update map when type or piste toggle changes
    LaunchedEffect(mapType, showPistes) {
        val map = osmMapView ?: return@LaunchedEffect
        rebuildMapOverlays(map, trackPoints, runNumber, mapType, showPistes, context)
        map.invalidate()
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                mapView.also { map ->
                    osmMapView = map
                    rebuildMapOverlays(map, trackPoints, runNumber, mapType, showPistes, context)
                    centerMapOnTrack(map, trackPoints)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Collapsible layers button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Layers icon button
                Surface(
                    shape = CircleShape,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    IconButton(
                        onClick = { showLayersMenu = !showLayersMenu },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            text = "\uD83D\uDDFA",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // Expanded layers menu
                if (showLayersMenu) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Map Type",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = mapType == TileSources.MapType.STANDARD,
                                    onClick = {
                                        mapType = TileSources.MapType.STANDARD
                                        osmMapView?.setTileSource(TileSources.OSM)
                                        osmMapView?.invalidate()
                                    },
                                    label = { Text("Map", style = MaterialTheme.typography.labelSmall) }
                                )
                                FilterChip(
                                    selected = mapType == TileSources.MapType.TOPOGRAPHIC,
                                    onClick = {
                                        mapType = TileSources.MapType.TOPOGRAPHIC
                                        osmMapView?.setTileSource(TileSources.TOPO)
                                        osmMapView?.invalidate()
                                    },
                                    label = { Text("Topo", style = MaterialTheme.typography.labelSmall) }
                                )
                                FilterChip(
                                    selected = mapType == TileSources.MapType.SATELLITE,
                                    onClick = {
                                        mapType = TileSources.MapType.SATELLITE
                                        osmMapView?.setTileSource(TileSources.SATELLITE)
                                        osmMapView?.invalidate()
                                    },
                                    label = { Text("Sat", style = MaterialTheme.typography.labelSmall) }
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Pistes",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Switch(
                                    checked = showPistes,
                                    onCheckedChange = { showPistes = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rebuild all map overlays (polyline, markers, piste layer)
 */
private fun rebuildMapOverlays(
    mapView: MapView,
    trackPoints: List<TrackPoint>,
    runNumber: Int,
    mapType: TileSources.MapType,
    showPistes: Boolean,
    context: android.content.Context
) {
    mapView.overlays.clear()

    if (trackPoints.isEmpty()) return

    // Set tile source
    mapView.setTileSource(
        when (mapType) {
            TileSources.MapType.STANDARD -> TileSources.OSM
            TileSources.MapType.TOPOGRAPHIC -> TileSources.TOPO
            TileSources.MapType.SATELLITE -> TileSources.SATELLITE
        }
    )

    // Add piste overlay if enabled
    if (showPistes) {
        val pisteProvider = MapTileProviderBasic(context, TileSources.SNOW)
        val pisteOverlay = TilesOverlay(pisteProvider, context).apply {
            loadingBackgroundColor = android.graphics.Color.TRANSPARENT
            loadingLineColor = android.graphics.Color.TRANSPARENT
        }
        mapView.overlays.add(pisteOverlay)
    }

    // Add speed-colored polyline segments
    val maxSpeed = trackPoints.maxOfOrNull { it.speed } ?: 1f
    for (i in 1 until trackPoints.size) {
        val prev = trackPoints[i - 1]
        val curr = trackPoints[i]
        val segmentPolyline = Polyline(mapView).apply {
            setPoints(listOf(
                GeoPoint(prev.latitude, prev.longitude),
                GeoPoint(curr.latitude, curr.longitude)
            ))
            val avgSpeed = (prev.speed + curr.speed) / 2f
            val normalized = (avgSpeed / maxSpeed).coerceIn(0f, 1f)
            val color = ChartUtils.getSpeedColor(normalized)
            outlinePaint.color = android.graphics.Color.argb(
                230,
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            outlinePaint.strokeWidth = 10f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
        }
        mapView.overlays.add(segmentPolyline)
    }

    // Start marker - green circle with run number
    val startPoint = trackPoints.first()
    val startMarker = Marker(mapView).apply {
        position = GeoPoint(startPoint.latitude, startPoint.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = createLabeledCircleDrawable(0xFF4CAF50.toInt(), runNumber.toString(), 48)
        title = "Start"
    }
    mapView.overlays.add(startMarker)

    // End marker - red circle with flag
    val endPoint = trackPoints.last()
    val endMarker = Marker(mapView).apply {
        position = GeoPoint(endPoint.latitude, endPoint.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = createLabeledCircleDrawable(0xFFF44336.toInt(), "\u2691", 48)
        title = "End"
    }
    mapView.overlays.add(endMarker)
}

/**
 * Center map on track bounds with padding
 */
private fun centerMapOnTrack(mapView: MapView, trackPoints: List<TrackPoint>) {
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

/**
 * Create a simple circle drawable for the position marker
 */
private fun createCircleDrawable(color: Int, sizePx: Int): Drawable {
    return object : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        override fun draw(canvas: Canvas) {
            val cx = bounds.exactCenterX()
            val cy = bounds.exactCenterY()
            val radius = sizePx / 2f
            canvas.drawCircle(cx, cy, radius, paint)
            canvas.drawCircle(cx, cy, radius, borderPaint)
        }

        override fun getIntrinsicWidth() = sizePx
        override fun getIntrinsicHeight() = sizePx
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            paint.colorFilter = colorFilter
        }
        @Deprecated("Deprecated in Java")
        override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
    }
}

/**
 * Create a circle drawable with text label (for start/end markers)
 */
private fun createLabeledCircleDrawable(color: Int, label: String, sizePx: Int): Drawable {
    return object : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            textSize = sizePx * 0.45f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        override fun draw(canvas: Canvas) {
            val cx = bounds.exactCenterX()
            val cy = bounds.exactCenterY()
            val radius = sizePx / 2f
            canvas.drawCircle(cx, cy, radius, paint)
            canvas.drawCircle(cx, cy, radius, borderPaint)
            val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(label, cx, textY, textPaint)
        }

        override fun getIntrinsicWidth() = sizePx
        override fun getIntrinsicHeight() = sizePx
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            paint.colorFilter = colorFilter
        }
        @Deprecated("Deprecated in Java")
        override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
    }
}
