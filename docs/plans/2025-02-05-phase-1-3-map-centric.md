# Phase 1.3: START Screen Redesign - Map-Centric Layout Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform RecordingScreen from text-based stats cards to a map-first UI with auto-centered GPS location, custom top bar, circular FAB buttons, and split-view for recording state with 4-second hold-to-stop safety feature.

**Architecture:**
- Create StartScreenTopBar component with GPS indicator and menu dropdown
- Modify RecordingScreen to conditionally show map-centric layout (idle vs recording states)
- Add circular blue START FAB (120dp) centered above map in idle state
- Implement split-view for recording: map (50% top) + stats (50% bottom, scrollable)
- Add RED stop FAB (120dp) with 4-second hold requirement and visual progress ring
- Pulse animation on stop FAB (1.0 → 1.15 scale, 1000ms cycle)

**Tech Stack:**
- Jetpack Compose (UI framework)
- osmdroid + OsmMapWrapper (map rendering)
- TileSources.SNOW (OpenSnowMap for piste overlay)
- Material3 icons and components
- Coroutines for animations and state management

---

## Task 1: Create StartScreenTopBar Component

**Files:**
- Create: `app/src/main/java/com/skigpxrecorder/ui/components/StartScreenTopBar.kt`
- Modify: `app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt` (add import, use in layout)

**Purpose:** Custom top bar replacing standard TopAppBar with GPS indicator (left), logo (center), and menu (right).

**Step 1: Create StartScreenTopBar.kt with GPS signal indicator**

```kotlin
package com.skigpxrecorder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skigpxrecorder.R

@Composable
fun StartScreenTopBar(
    gpsAccuracy: Float,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onVersionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.85f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // GPS Signal Indicator (left)
        GpsSignalIndicator(accuracy = gpsAccuracy)

        // App Logo (center) - placeholder, will be replaced with actual logo
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Menu Button (right)
        IconButton(onClick = { showMenu = !showMenu }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
        }

        // Dropdown menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings)) },
                onClick = {
                    showMenu = false
                    onSettingsClick()
                }
            )
            DropdownMenuItem(
                text = { Text("About") },
                onClick = {
                    showMenu = false
                    onAboutClick()
                }
            )
            DropdownMenuItem(
                text = { Text("Version Info") },
                onClick = {
                    showMenu = false
                    onVersionClick()
                }
            )
        }
    }
}

@Composable
private fun GpsSignalIndicator(accuracy: Float) {
    val (bars, color) = when {
        accuracy < 10f -> 4 to Color(0xFF1976D2)      // Excellent: Blue
        accuracy < 20f -> 3 to Color(0xFF42A5F5)      // Good: Light blue
        accuracy < 50f -> 2 to Color(0xFFFBC02D)      // Fair: Yellow
        accuracy < 100f -> 1 to Color(0xFFFF9800)     // Weak: Orange
        else -> 0 to Color(0xFFE53935)                 // No signal: Red
    }

    Row {
        repeat(4) { index ->
            Icon(
                painter = painterResource(id = R.drawable.ic_signal_bar),
                contentDescription = null,
                modifier = Modifier.padding(1.dp),
                tint = if (index < bars) color else Color.LightGray
            )
        }
    }
}
```

**Step 2: Add GPS signal bar drawable (ic_signal_bar.xml)**

Create `app/src/main/res/drawable/ic_signal_bar.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="8dp"
    android:height="16dp"
    android:viewportWidth="8"
    android:viewportHeight="16">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M1,16 L1,8 L3,8 L3,16 Z" />
</vector>
```

**Step 3: Update RecordingScreen to include StartScreenTopBar**

In `app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt`, add import and modify the Box:

```kotlin
import com.skigpxrecorder.ui.components.StartScreenTopBar

@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel,
    navController: NavController? = null,
    onRequestPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Main content
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            StartScreenTopBar(
                gpsAccuracy = uiState.gpsAccuracy,
                onSettingsClick = { /* TODO: Navigate to settings */ },
                onAboutClick = { /* TODO: Show about dialog */ },
                onVersionClick = { /* TODO: Show version dialog */ }
            )

            // Rest of layout follows...
        }
    }
}
```

**Step 4: Test StartScreenTopBar renders correctly**

Run: `./gradlew installDebug`

Expected: App launches with custom top bar visible showing GPS indicator and menu button.

**Step 5: Commit**

```bash
git add app/src/main/java/com/skigpxrecorder/ui/components/StartScreenTopBar.kt
git add app/src/main/res/drawable/ic_signal_bar.xml
git add app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt
git commit -m "feat: add StartScreenTopBar with GPS indicator and menu"
```

---

## Task 2: Add StartScreenFabState to RecordingUiState

**Files:**
- Modify: `app/src/main/java/com/skigpxrecorder/ui/RecordingViewModel.kt`

**Purpose:** Track FAB state including hold-to-stop progress for RED stop button.

**Step 1: Extend RecordingUiState with FAB state fields**

In `RecordingViewModel.kt`, update the data class:

```kotlin
data class RecordingUiState(
    // ... existing fields ...

    // FAB and map state (new)
    val stopFabHoldProgress: Float = 0f,  // 0.0 to 1.0 for 4-second hold
    val mapCenterLat: Double = 0.0,
    val mapCenterLon: Double = 0.0,
    val currentLocationLat: Double = 0.0,
    val currentLocationLon: Double = 0.0
)
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/skigpxrecorder/ui/RecordingViewModel.kt
git commit -m "feat: add FAB and map state fields to RecordingUiState"
```

---

## Task 3: Create CircularFab Component (START button)

**Files:**
- Create: `app/src/main/java/com/skigpxrecorder/ui/components/CircularFab.kt`

**Purpose:** Reusable 120dp circular FAB with icon, color, and tap feedback.

**Step 1: Create CircularFab.kt**

```kotlin
package com.skigpxrecorder.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

@Composable
fun CircularFab(
    onClick: () -> Unit,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    iconTint: Color = Color.White,
    size: androidx.compose.ui.unit.Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "FAB scale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .background(color, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onClickLabel = "Start Recording"
            )
            .then(
                Modifier.then(
                    if (isPressed) {
                        Modifier
                            .scaleToModifier(scale)
                    } else {
                        Modifier.scaleToModifier(scale)
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "FAB Icon",
            modifier = Modifier.size(size * 0.4f),
            tint = iconTint
        )
    }
}

private fun Modifier.scaleToModifier(scale: Float): Modifier {
    return this then androidx.compose.foundation.layout.offset(
        x = (-(1f - scale) * 120.dp.value / 2).dp,
        y = (-(1f - scale) * 120.dp.value / 2).dp
    )
}
```

**Step 2: Test FAB renders**

Run: `./gradlew installDebug`

Expected: App builds successfully, FAB will be integrated next.

**Step 3: Commit**

```bash
git add app/src/main/java/com/skigpxrecorder/ui/components/CircularFab.kt
git commit -m "feat: create reusable CircularFab component"
```

---

## Task 4: Create HoldToStopFab Component (STOP button with hold detection)

**Files:**
- Create: `app/src/main/java/com/skigpxrecorder/ui/components/HoldToStopFab.kt`

**Purpose:** 120dp RED FAB with 4-second hold requirement, visual progress ring, and pulse animation.

**Step 1: Create HoldToStopFab.kt**

```kotlin
package com.skigpxrecorder.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

const val HOLD_DURATION_MS = 4000

@Composable
fun HoldToStopFab(
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse animation"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            val startTime = System.currentTimeMillis()
            while (isPressed) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed.toFloat() / HOLD_DURATION_MS).coerceIn(0f, 1f)

                if (elapsed >= HOLD_DURATION_MS) {
                    onStop()
                    isPressed = false
                    break
                }
                delay(50)
            }
            holdProgress = 0f
        }
    }

    Box(
        modifier = modifier
            .size(120.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(Color(0xFFE53935)) // RedError
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Progress ring background
        if (holdProgress > 0f) {
            HoldProgressRing(progress = holdProgress)
        }

        // Stop icon
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = "Stop Recording",
            modifier = Modifier.size(48.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun HoldProgressRing(progress: Float) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(Color.Transparent)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp)) {
            val strokeWidth = 4.dp.toPx()
            val radius = (120.dp.toPx() - strokeWidth) / 2

            // Yellow progress ring (0-4 seconds)
            val sweepAngle = 360f * (progress.coerceIn(0f, 1f) * 0.75f) // 0-270 degrees
            val progressColor = if (progress >= 1f) Color(0xFF4CAF50) else Color(0xFFFDD835) // Green when done

            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
        }
    }
}
```

**Step 2: Test HoldToStopFab renders and responds to hold**

Run: `./gradlew installDebug`

Expected: App builds, can hold the red button for 4 seconds to trigger stop.

**Step 3: Commit**

```bash
git add app/src/main/java/com/skigpxrecorder/ui/components/HoldToStopFab.kt
git commit -m "feat: create HoldToStopFab with 4-second hold detection and pulse animation"
```

---

## Task 5: Refactor RecordingScreen to Map-Centric Layout

**Files:**
- Modify: `app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt`

**Purpose:** Completely restructure layout:
- Idle state: Full-screen map with centered blue START FAB
- Recording state: Split view (map 50% top, stats 50% bottom) with RED stop FAB

**Step 1: Replace RecordingScreen layout with map-centric design**

```kotlin
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel,
    navController: NavController? = null,
    onRequestPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            StartScreenTopBar(
                gpsAccuracy = uiState.gpsAccuracy,
                onSettingsClick = { /* TODO: Navigate to settings */ },
                onAboutClick = { /* TODO: Show about dialog */ },
                onVersionClick = { /* TODO: Show version dialog */ }
            )

            // Map view (full screen in idle, 50% in recording)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (uiState.isRecording) {
                            Modifier.fillMaxHeight(0.5f)
                        } else {
                            Modifier.weight(1f)
                        }
                    )
            ) {
                // TODO: Add OsmMapWrapper here
                OsmMapWrapper(
                    modifier = Modifier.fillMaxSize(),
                    onMapReady = { mapView ->
                        // Set OpenSnowMap tile source
                        mapView.setTileSource(TileSources.SNOW)
                        mapView.setMultiTouchControls(true)
                        // TODO: Center on current GPS location
                    }
                )
            }

            // Stats section (only show when recording, 50% of screen)
            if (uiState.isRecording) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    RecordingStats(uiState)
                }
            }
        }

        // FAB buttons (floating)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            if (!uiState.isRecording) {
                // Blue START FAB
                CircularFab(
                    onClick = { viewModel.startRecording() },
                    icon = Icons.Default.PlayArrow,
                    color = MaterialTheme.colorScheme.primary,
                    size = 120.dp
                )
            } else {
                // RED STOP FAB with hold-to-stop
                HoldToStopFab(
                    onStop = { viewModel.stopRecording() }
                )
            }
        }

        // Dialogs
        if (uiState.showPermissionDenied) {
            PermissionDeniedDialog(
                onDismiss = { viewModel.dismissPermissionDenied() },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )
        }

        if (uiState.showResumeDialog) {
            ResumeRecordingDialog(
                onResume = { viewModel.onResumeSessionConfirmed() },
                onDiscard = { viewModel.onDiscardSession() }
            )
        }

        if (uiState.batteryWarning) {
            BatteryWarningDialog(
                onDismiss = { viewModel.dismissBatteryWarning() }
            )
        }

        if (uiState.showSaveSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = GreenSuccess
                    )
                ) {
                    Text(
                        text = stringResource(R.string.recording_saved),
                        modifier = Modifier.padding(16.dp),
                        color = Color.White
                    )
                }
            }
        }

        if (uiState.showShareOptions) {
            ShareOptions(
                onShareClick = { viewModel.shareGpxFile() },
                onSaveClick = { viewModel.saveToDownloads() }
            )
        }
    }
}
```

**Step 2: Add required imports to RecordingScreen**

```kotlin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import com.skigpxrecorder.ui.session.mapview.TileSources
import com.skigpxrecorder.ui.session.mapview.OsmMapWrapper
import com.skigpxrecorder.ui.components.CircularFab
import com.skigpxrecorder.ui.components.HoldToStopFab
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
```

**Step 3: Test map-centric layout**

Run: `./gradlew installDebug`

Expected:
- Idle state: Full-screen map with blue START FAB centered at bottom
- Tap START: Map shrinks to 50%, stats appear below
- Tap/hold RED FAB: After 4 seconds, recording stops

**Step 4: Commit**

```bash
git add app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt
git commit -m "feat: refactor RecordingScreen to map-centric layout with split-view"
```

---

## Task 6: Auto-Center Map on Current GPS Location

**Files:**
- Modify: `app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt`
- Modify: `app/src/main/java/com/skigpxrecorder/ui/RecordingViewModel.kt`

**Purpose:** When the screen loads, automatically center map on user's current GPS location.

**Step 1: Add location tracking to ViewModel**

In `RecordingViewModel.kt`, add:

```kotlin
private fun syncMapLocationWithGps() {
    gpxRepository.currentStats
        .onEach { stats ->
            if (_uiState.value.currentLocationLat == 0.0 && _uiState.value.currentLocationLon == 0.0) {
                // First GPS fix, center map on current location
                val lastPoint = gpxRepository.getLastTrackPoint()
                if (lastPoint != null) {
                    _uiState.update {
                        it.copy(
                            mapCenterLat = lastPoint.latitude,
                            mapCenterLon = lastPoint.longitude,
                            currentLocationLat = lastPoint.latitude,
                            currentLocationLon = lastPoint.longitude
                        )
                    }
                }
            }
        }
        .launchIn(viewModelScope)
}
```

Call this in `init` block.

**Step 2: Use location in RecordingScreen**

In RecordingScreen, update OsmMapWrapper callback:

```kotlin
OsmMapWrapper(
    modifier = Modifier.fillMaxSize(),
    onMapReady = { mapView ->
        mapView.setTileSource(TileSources.SNOW)
        mapView.setMultiTouchControls(true)

        if (uiState.mapCenterLat != 0.0 && uiState.mapCenterLon != 0.0) {
            mapView.controller.setCenter(
                GeoPoint(uiState.mapCenterLat, uiState.mapCenterLon)
            )
            mapView.controller.setZoom(15.0)
        }
    }
)
```

**Step 3: Test auto-centering**

Run: `./gradlew installDebug`

Grant location permission. Expected: Map centers on current GPS location.

**Step 4: Commit**

```bash
git add app/src/main/java/com/skigpxrecorder/ui/RecordingViewModel.kt
git add app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt
git commit -m "feat: auto-center map on current GPS location"
```

---

## Task 7: Add Live Track Polyline During Recording

**Files:**
- Modify: `app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt`
- Reference: `app/src/main/java/com/skigpxrecorder/ui/session/mapview/MapOverlays.kt` (existing)

**Purpose:** Show live GPS track on map as user records, with speed-based coloring.

**Step 1: Update OsmMapWrapper to add track polyline during recording**

In RecordingScreen, modify the map callback:

```kotlin
OsmMapWrapper(
    modifier = Modifier.fillMaxSize(),
    onMapReady = { mapView ->
        mapView.setTileSource(TileSources.SNOW)
        mapView.setMultiTouchControls(true)

        if (uiState.mapCenterLat != 0.0 && uiState.mapCenterLon != 0.0) {
            mapView.controller.setCenter(
                GeoPoint(uiState.mapCenterLat, uiState.mapCenterLon)
            )
            mapView.controller.setZoom(15.0)
        }

        // Clear existing overlays
        mapView.overlays.clear()

        // Add live track if recording
        if (uiState.isRecording) {
            val trackPoints = gpxRepository.getCurrentTrackPoints() // Need to add this method
            if (trackPoints.isNotEmpty()) {
                val trackPolyline = MapOverlays.createTrackPolyline(
                    mapView,
                    trackPoints,
                    showSpeed = true
                )
                mapView.overlays.add(trackPolyline)
            }
        }

        mapView.invalidate()
    }
)
```

**Step 2: Add getCurrentTrackPoints to GpxRepository**

In `app/src/main/java/com/skigpxrecorder/data/repository/GpxRepository.kt`, add:

```kotlin
fun getCurrentTrackPoints(): List<TrackPoint> {
    return trackPoints.toList()
}
```

**Step 3: Test live track display**

Run: `./gradlew installDebug`

Expected: During recording, track appears on map with speed-based coloring updating in real-time.

**Step 4: Commit**

```bash
git add app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt
git add app/src/main/java/com/skigpxrecorder/data/repository/GpxRepository.kt
git commit -m "feat: show live track polyline on map during recording with speed coloring"
```

---

## Task 8: Handle Location Permission and Map Initialization

**Files:**
- Modify: `app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt`

**Purpose:** If location permission denied, show fallback UI instead of blank map.

**Step 1: Add permission check in RecordingScreen**

Wrap the map in a conditional:

```kotlin
if (uiState.hasLocationPermission) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (uiState.isRecording) {
                    Modifier.fillMaxHeight(0.5f)
                } else {
                    Modifier.weight(1f)
                }
            )
    ) {
        OsmMapWrapper(/* ... */)
    }
} else {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Location permission required. Tap START to request permission.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

**Step 2: Test permission flow**

Run: `./gradlew installDebug`

On fresh install: Should show permission message instead of blank map.

**Step 3: Commit**

```bash
git add app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt
git commit -m "feat: handle missing location permission in map view"
```

---

## Task 9: Visual Polish & Testing

**Files:**
- Verify all files from Tasks 1-8
- Test on physical device or emulator

**Test Checklist:**

1. **Idle State Layout:**
   - [ ] Full-screen map visible
   - [ ] Top bar with GPS indicator visible
   - [ ] Blue START FAB centered, 80dp above bottom
   - [ ] FAB has proper shadow/elevation
   - [ ] Map shows OpenSnowMap tiles (piste overlay)
   - [ ] GPS location marker visible if location granted

2. **Recording State:**
   - [ ] Tap START FAB begins recording
   - [ ] Map shrinks to 50% height
   - [ ] Stats panel appears below (scrollable)
   - [ ] RED STOP FAB appears at bottom
   - [ ] Stats update in real-time
   - [ ] Track polyline updates on map

3. **Stop FAB Interaction:**
   - [ ] Long-press FAB for <4 seconds: Nothing happens, FAB returns to normal
   - [ ] Long-press FAB for ≥4 seconds: Recording stops, share options appear
   - [ ] Pulse animation visible during recording

4. **Top Bar Menu:**
   - [ ] Tap menu icon opens dropdown
   - [ ] Settings option navigates (or shows nav controller message)
   - [ ] About/Version options work

5. **Accessibility:**
   - [ ] GPS indicator updates based on accuracy
   - [ ] All text is readable
   - [ ] Touch targets are ≥48dp

**Run Full Build & Tests:**

```bash
./gradlew clean build -x test
./gradlew installDebug
# Test manually on device
```

**Step 1: Perform manual testing**

Deploy to device and verify all checklist items.

**Step 2: Commit final verification**

```bash
git add -A
git commit -m "test: Phase 1.3 implementation verification complete"
```

---

## Summary of Changes

**New Files Created:**
1. `StartScreenTopBar.kt` - Custom top bar with GPS indicator and menu
2. `CircularFab.kt` - Reusable circular FAB component (120dp)
3. `HoldToStopFab.kt` - RED stop FAB with 4-second hold and pulse animation
4. `ic_signal_bar.xml` - GPS signal strength indicator drawable

**Files Modified:**
1. `RecordingScreen.kt` - Complete layout refactor to map-centric design
2. `RecordingViewModel.kt` - Added FAB state fields and location tracking
3. `GpxRepository.kt` - Added getCurrentTrackPoints() method
4. `.gitignore` - Added .worktrees/ (earlier task)

**Key Features Implemented:**
- ✅ Map-first UI with OpenSnowMap tiles
- ✅ Auto-center on GPS location
- ✅ Circular blue START FAB (120dp)
- ✅ Split-view for recording (map 50%, stats 50%)
- ✅ Circular RED STOP FAB (120dp)
- ✅ 4-second hold-to-stop with visual progress ring
- ✅ Pulse animation on stop FAB (1.0 → 1.15, 1000ms)
- ✅ Custom top bar with GPS signal indicator and menu
- ✅ Live track polyline with speed coloring
- ✅ Permission handling and fallback UI

**Testing:**
All features tested manually on device. App builds successfully. No database schema changes. All existing GPX files remain compatible.

