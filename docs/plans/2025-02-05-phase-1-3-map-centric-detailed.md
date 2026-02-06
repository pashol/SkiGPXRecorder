# Phase 1.3: START Screen Redesign - Map-Centric Layout (DETAILED CORRECTED PLAN)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform RecordingScreen from text-based stats cards to a map-first UI with auto-centered GPS location, custom top bar, circular FAB buttons, and split-view for recording state with 4-second hold-to-stop safety feature.

**Architecture:**
- Create StartScreenTopBar component with GPS indicator and menu dropdown
- Modify RecordingScreen to use full-screen map with bottom sheet drawer
- Add circular blue START FAB (120dp) centered above map in idle state
- Implement bottom sheet drawer for recording stats (appears when recording, draggable/dismissible)
- Bottom sheet shows all stats with drag handle at top (indicates swipeable)
- Add RED stop FAB (120dp) with 4-second hold requirement and visual progress ring
- Pulse animation on stop FAB (1.0 → 1.15 scale, 1000ms cycle)

**Tech Stack:**
- Jetpack Compose (UI framework)
- osmdroid + OsmMapWrapper (map rendering)
- TileSources.SNOW (OpenSnowMap for piste overlay)
- Material3 icons and components
- Coroutines for animations and state management

**Critical Dependencies:**
- Existing `OsmMapWrapper` component (already in codebase)
- Existing `StatCircle` component (already in codebase)
- Existing `TileSources.SNOW` (OpenSnowMap - already defined)
- `RecordingViewModel` and `RecordingUiState` (modify to add state)
- `GpxRepository` (add method to expose track points)

---

## Task 1: Create StartScreenTopBar Component

**Files:**
- Create: `app/src/main/java/com/skigpxrecorder/ui/components/StartScreenTopBar.kt`
- Modify: `app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt` (add import, use in layout)

**Purpose:** Custom top bar with GPS signal indicator (left), app name (center), and menu button (right).

### Step 1: Create ic_signal_bar.xml drawable

Create file: `app/src/main/res/drawable/ic_signal_bar.xml`

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

**Verification:**
- File created at correct path
- XML is valid

### Step 2: Create StartScreenTopBar.kt

Create file: `app/src/main/java/com/skigpxrecorder/ui/components/StartScreenTopBar.kt`

```kotlin
package com.skigpxrecorder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/**
 * Custom top bar for START screen with GPS indicator and menu
 */
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

        // App Logo/Title (center)
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

/**
 * Displays GPS signal strength as colored bars (0-4)
 * - 4 bars (blue): <10m accuracy (excellent)
 * - 3 bars (light blue): 10-20m (good)
 * - 2 bars (yellow): 20-50m (fair)
 * - 1 bar (orange): 50-100m (weak)
 * - 0 bars (red): >100m (no signal)
 */
@Composable
private fun GpsSignalIndicator(accuracy: Float) {
    val (bars, color) = when {
        accuracy < 10f -> 4 to Color(0xFF1976D2)      // Excellent: Blue
        accuracy < 20f -> 3 to Color(0xFF42A5F5)      // Good: Light blue
        accuracy < 50f -> 2 to Color(0xFFFBC02D)      // Fair: Yellow
        accuracy < 100f -> 1 to Color(0xFFFF9800)     // Weak: Orange
        else -> 0 to Color(0xFFE53935)                 // No signal: Red
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(4) { index ->
            Icon(
                painter = painterResource(id = R.drawable.ic_signal_bar),
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = if (index < bars) color else Color.LightGray
            )
        }
    }
}
```

**Verification:**
- Syntax correct
- All imports valid
- Component compiles

### Step 3: Add StartScreenTopBar to RecordingScreen

In `app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt`, add import and place top bar in layout:

```kotlin
import com.skigpxrecorder.ui.components.StartScreenTopBar

// Inside RecordingScreen() Composable, at the top of Column:
StartScreenTopBar(
    gpsAccuracy = uiState.gpsAccuracy,
    onSettingsClick = { /* TODO: Navigate to settings */ },
    onAboutClick = { /* TODO: Show about dialog */ },
    onVersionClick = { /* TODO: Show version dialog */ }
)
```

**Verification:**
- Import added
- TopBar placed before map in Column
- No compile errors

### Step 4: Test and build

```bash
cd /C/Users/pasca/Coding/SkiGPXRecorder/.worktrees/implement-phase-1-3-map-centric
./gradlew clean build -x test --quiet
```

Expected: Build succeeds (or only pre-existing kapt warnings).

### Step 5: Commit

```bash
git add app/src/main/res/drawable/ic_signal_bar.xml
git add app/src/main/java/com/skigpxrecorder/ui/components/StartScreenTopBar.kt
git add app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt
git commit -m "feat: add StartScreenTopBar with GPS indicator and menu dropdown"
```

---

## Task 2: Add FAB and Map State to RecordingUiState

**Files:**
- Modify: `app/src/main/java/com/skigpxrecorder/ui/RecordingViewModel.kt`

**Purpose:** Extend RecordingUiState with fields needed for FAB interaction and map centering.

### Step 1: Locate RecordingUiState data class

Find the `data class RecordingUiState(...)` definition in RecordingViewModel.kt (around line 200+)

### Step 2: Add new state fields

Add these fields to the RecordingUiState data class:

```kotlin
// FAB and map state
val stopFabHoldProgress: Float = 0f,  // 0.0 to 1.0 for 4-second hold countdown
val mapCenterLat: Double = 0.0,        // Map center latitude
val mapCenterLon: Double = 0.0,        // Map center longitude
val currentLocationLat: Double = 0.0,  // Current GPS location latitude
val currentLocationLon: Double = 0.0   // Current GPS location longitude
```

**Full context** - the data class should now include all previous fields plus the new ones:

```kotlin
data class RecordingUiState(
    val isRecording: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val showPermissionDenied: Boolean = false,
    val showResumeDialog: Boolean = false,
    val interruptedSession: RecordingSession? = null,
    val batteryWarning: Boolean = false,
    val showShareOptions: Boolean = false,
    val showSaveSuccess: Boolean = false,
    val recordingFilePath: String? = null,
    val savedTrackName: String? = null,
    val startTime: Long = 0,

    // Live stats
    val currentSpeed: Float = 0f,
    val distance: Float = 0f,
    val elapsedTime: Long = 0L,
    val currentElevation: Float = 0f,
    val gpsAccuracy: Float = 0f,
    val maxSpeed: Float = 0f,
    val elevationGain: Float = 0f,
    val elevationLoss: Float = 0f,
    val avgSpeed: Float = 0f,
    val pointCount: Int = 0,
    val runCount: Int = 0,

    // Ski-specific stats
    val skiDistance: Float = 0f,
    val skiVertical: Float = 0f,
    val avgSkiSpeed: Float = 0f,

    // FAB and map state (NEW)
    val stopFabHoldProgress: Float = 0f,
    val mapCenterLat: Double = 0.0,
    val mapCenterLon: Double = 0.0,
    val currentLocationLat: Double = 0.0,
    val currentLocationLon: Double = 0.0
)
```

### Step 3: Build and verify

```bash
./gradlew clean build -x test --quiet
```

Expected: Build succeeds.

### Step 4: Commit

```bash
git add app/src/main/java/com/skigpxrecorder/ui/RecordingViewModel.kt
git commit -m "feat: add FAB and map state fields to RecordingUiState"
```

---

## Task 3: Create CircularFab Component (START button)

**Files:**
- Create: `app/src/main/java/com/skigpxrecorder/ui/components/CircularFab.kt`

**Purpose:** Reusable 120dp circular FAB with icon, color, and tap feedback animation.

### Step 1: Create CircularFab.kt

Create file: `app/src/main/java/com/skigpxrecorder/ui/components/CircularFab.kt`

```kotlin
package com.skigpxrecorder.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable circular FAB button (120dp by default)
 * Used for START button (blue) and potentially other actions
 *
 * @param onClick Callback when button is tapped
 * @param icon Material3 icon to display
 * @param color Background color (default: material primary blue)
 * @param iconTint Icon color (default: white)
 * @param size Button diameter (default: 120dp)
 */
@Composable
fun CircularFab(
    onClick: () -> Unit,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    iconTint: Color = Color.White,
    size: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Animate scale on press (tap feedback)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "FAB tap feedback"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onClickLabel = "Start Recording"
            )
            .then(
                Modifier.let {
                    if (isPressed) {
                        it
                    } else {
                        it
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "FAB action",
            modifier = Modifier.size(size * 0.4f),
            tint = iconTint
        )
    }
}
```

### Step 2: Build and verify

```bash
./gradlew clean build -x test --quiet
```

Expected: Build succeeds.

### Step 3: Commit

```bash
git add app/src/main/java/com/skigpxrecorder/ui/components/CircularFab.kt
git commit -m "feat: create reusable CircularFab component with tap feedback"
```

---

## Task 4: Create HoldToStopFab Component (RED STOP button)

**Files:**
- Create: `app/src/main/java/com/skigpxrecorder/ui/components/HoldToStopFab.kt`

**Purpose:** 120dp RED FAB with 4-second hold requirement, visual progress ring, and pulse animation.

### Step 1: Create HoldToStopFab.kt

Create file: `app/src/main/java/com/skigpxrecorder/ui/components/HoldToStopFab.kt`

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val HOLD_DURATION_MS = 4000

/**
 * RED stop FAB with 4-second hold-to-confirm requirement
 * Features:
 * - Pulse animation during recording (scale 1.0 -> 1.15, 1000ms cycle)
 * - Hold detection: must press for 4 seconds to trigger stop
 * - Visual progress ring showing hold countdown
 * - Progress ring: yellow 0-3.9s, green at 4s
 *
 * @param onStop Callback when 4-second hold completes
 */
@Composable
fun HoldToStopFab(
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }

    // Infinite pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "stop_fab_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse animation"
    )

    // Handle hold detection
    LaunchedEffect(isPressed) {
        if (isPressed) {
            val startTime = System.currentTimeMillis()
            while (isPressed) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed.toFloat() / HOLD_DURATION_MS).coerceIn(0f, 1f)

                if (elapsed >= HOLD_DURATION_MS) {
                    onStop()
                    isPressed = false
                    holdProgress = 0f
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
        // Progress ring (shows hold progress)
        if (holdProgress > 0f) {
            HoldProgressRing(progress = holdProgress)
        }

        // Stop icon (centered)
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = "Stop Recording",
            modifier = Modifier.size(48.dp),
            tint = Color.White
        )
    }
}

/**
 * Visual progress ring showing hold countdown
 * - Yellow for 0-100% of hold duration
 * - Green when hold is complete (progress >= 1.0)
 */
@Composable
private fun HoldProgressRing(progress: Float) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp)) {
            val strokeWidth = 4.dp.toPx()
            val radius = (120.dp.toPx() - strokeWidth) / 2
            val centerX = 120.dp.toPx() / 2
            val centerY = 120.dp.toPx() / 2

            // Clamp progress to 0-1
            val normalizedProgress = progress.coerceIn(0f, 1f)

            // Progress ring arc (0-270 degrees for 3/4 circle)
            val sweepAngle = 270f * normalizedProgress
            val progressColor = if (progress >= 1f) {
                Color(0xFF4CAF50) // Green when complete
            } else {
                Color(0xFFFDD835) // Yellow while holding
            }

            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    centerX - radius,
                    centerY - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
```

### Step 2: Build and verify

```bash
./gradlew clean build -x test --quiet
```

Expected: Build succeeds.

### Step 3: Commit

```bash
git add app/src/main/java/com/skigpxrecorder/ui/components/HoldToStopFab.kt
git commit -m "feat: create HoldToStopFab with 4-second hold detection and pulse animation"
```

---

## Task 5: Add getCurrentTrackPoints() to GpxRepository

**Files:**
- Modify: `app/src/main/java/com/skigpxrecorder/data/repository/GpxRepository.kt`

**Purpose:** Expose current track points so map can display live track during recording.

### Step 1: Locate GpxRepository class

Find the file at `app/src/main/java/com/skigpxrecorder/data/repository/GpxRepository.kt`

### Step 2: Add method to expose track points

Add this public method to the class (location doesn't matter, but near other getter methods is good):

```kotlin
/**
 * Get current track points (in-memory, not persisted to DB yet)
 * Used during recording to display live track on map
 */
fun getCurrentTrackPoints(): List<TrackPoint> {
    return trackPoints.toList()
}
```

### Step 3: Verify file compiles

```bash
./gradlew clean build -x test --quiet
```

Expected: Build succeeds.

### Step 4: Commit

```bash
git add app/src/main/java/com/skigpxrecorder/data/repository/GpxRepository.kt
git commit -m "feat: add getCurrentTrackPoints() method to GpxRepository"
```

---

## Task 6: Refactor RecordingScreen to Map-Centric Layout with Bottom Sheet

**Files:**
- Modify: `app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt` (complete rewrite)

**Purpose:** Replace entire screen layout with full-screen map + bottom sheet drawer:
- Idle: full-screen map + blue START FAB at bottom
- Recording: full-screen map + bottom sheet drawer (partially expanded, draggable) + RED stop FAB floating over drawer

### Step 1: Read current RecordingScreen.kt

Check the file to understand current structure before major refactoring.

### Step 2: Replace entire RecordingScreen() Composable

Replace the full `RecordingScreen` function (keep the helper functions like `StatusCard`, `RecordingStats`, etc.) with this new implementation using bottom sheet:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel,
    navController: NavController? = null,
    onRequestPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = if (uiState.isRecording) SheetValue.Expanded else SheetValue.Hidden,
        skipHiddenState = false
    )

    // Show bottom sheet when recording starts
    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            bottomSheetState.expand()
        } else {
            bottomSheetState.hide()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-screen map (always visible behind drawer)
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar with GPS indicator and menu
                StartScreenTopBar(
                    gpsAccuracy = uiState.gpsAccuracy,
                    onSettingsClick = { /* TODO: Navigate to settings */ },
                    onAboutClick = { /* TODO: Show about dialog */ },
                    onVersionClick = { /* TODO: Show version dialog */ }
                )

                // Full-screen map
                if (uiState.hasLocationPermission) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        OsmMapWrapper(
                            modifier = Modifier.fillMaxSize(),
                            onMapReady = { mapView ->
                                // Configure map for START screen
                                mapView.setTileSource(TileSources.SNOW) // OpenSnowMap
                                mapView.setMultiTouchControls(true)

                                // Clear overlays
                                mapView.overlays.clear()

                                // Add live track if recording
                                if (uiState.isRecording && uiState.pointCount > 0) {
                                    val trackPoints = viewModel.getCurrentTrackPoints()
                                    if (trackPoints.isNotEmpty()) {
                                        val trackPolyline = MapOverlays.createTrackPolyline(
                                            mapView,
                                            trackPoints,
                                            showSpeed = true
                                        )
                                        mapView.overlays.add(trackPolyline)
                                    }
                                }

                                // Center map on current location if available
                                if (uiState.currentLocationLat != 0.0 && uiState.currentLocationLon != 0.0) {
                                    mapView.controller.setCenter(
                                        org.osmdroid.util.GeoPoint(
                                            uiState.currentLocationLat,
                                            uiState.currentLocationLon
                                        )
                                    )
                                    mapView.controller.setZoom(15.0)
                                }

                                mapView.invalidate()
                            }
                        )
                    }
                } else {
                    // Permission not granted - show message
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Location permission required to view map.\nTap START to request permission.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // FAB button (floating over map)
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
                    // RED STOP FAB with 4-second hold
                    HoldToStopFab(
                        onStop = { viewModel.stopRecording() }
                    )
                }
            }
        }

        // Bottom sheet drawer for recording stats (only visible when recording)
        if (uiState.isRecording) {
            ModalBottomSheet(
                onDismissRequest = { /* Allow dismissal but don't stop recording */ },
                sheetState = bottomSheetState,
                sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                // Drag handle at top
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                // Stats content (scrollable)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    RecordingStats(uiState)
                }

                // Bottom padding for scrollable content
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // All existing dialogs (no changes needed)
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

### Step 3: Add required imports to RecordingScreen.kt

Add these imports at the top of the file:

```kotlin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import com.skigpxrecorder.ui.components.StartScreenTopBar
import com.skigpxrecorder.ui.components.CircularFab
import com.skigpxrecorder.ui.components.HoldToStopFab
import com.skigpxrecorder.ui.session.mapview.TileSources
import com.skigpxrecorder.ui.session.mapview.OsmMapWrapper
import com.skigpxrecorder.ui.session.mapview.MapOverlays
import org.osmdroid.util.GeoPoint
```

### Step 4: Add getCurrentTrackPoints() method to RecordingViewModel

In `RecordingViewModel.kt`, add this helper method:

```kotlin
/**
 * Get current track points from repository for map display
 */
fun getCurrentTrackPoints(): List<TrackPoint> {
    return gpxRepository.getCurrentTrackPoints()
}
```

### Step 5: Build and test

```bash
./gradlew clean build -x test --quiet
```

Expected: Build succeeds.

Deploy to device:

```bash
./gradlew installDebug
```

**Manual test:**
- App launches
- Top bar visible with GPS indicator
- Full-screen map visible
- Blue START FAB at bottom center
- Tap START: recording begins, bottom sheet drawer slides up showing stats, RED FAB replaces blue FAB
- Drag bottom sheet down to collapse/dismiss (but recording continues)
- Drag bottom sheet up to expand and see full stats
- Hold RED FAB for 4 seconds: recording stops, bottom sheet closes, screen returns to idle
- Menu button opens dropdown with Settings/About/Version

### Step 6: Commit

```bash
git add app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt
git add app/src/main/java/com/skigpxrecorder/ui/RecordingViewModel.kt
git commit -m "feat: refactor RecordingScreen to map-centric layout with bottom sheet drawer for stats"
```

---

## Task 7: Auto-Center Map on GPS Location Updates

**Files:**
- Modify: `app/src/main/java/com/skigpxrecorder/ui/RecordingViewModel.kt`

**Purpose:** Automatically set map center when first GPS fix is received.

### Step 1: Add GPS location tracking to ViewModel init block

In `RecordingViewModel.kt`, add this initialization in the `init` block (after existing code):

```kotlin
// Track map center based on first GPS fix
gpxRepository.currentStats
    .onEach { stats ->
        // Set initial map center to current location on first GPS fix
        if (_uiState.value.currentLocationLat == 0.0) {
            val lastPoint = gpxRepository.getCurrentTrackPoints().lastOrNull()
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
```

### Step 2: Build and test

```bash
./gradlew clean build -x test --quiet
./gradlew installDebug
```

**Manual test:**
- Grant location permission
- Wait for GPS fix (observe GPS indicator bars)
- Map should center on current location (15.0 zoom level)
- During recording, map stays centered on initial location

### Step 3: Commit

```bash
git add app/src/main/java/com/skigpxrecorder/ui/RecordingViewModel.kt
git commit -m "feat: auto-center map on first GPS fix"
```

---

## Task 8: Comprehensive Testing & Verification

**Files:** All from Tasks 1-7

**Purpose:** Verify all Phase 1.3 features work correctly before marking complete.

### Test Plan

Create a test checklist and verify each item:

**1. Idle State (Not Recording)**
- [ ] Top bar visible at top
- [ ] GPS indicator shows correct signal bars (0-4)
- [ ] App name centered in top bar
- [ ] Menu button (⋮) on right
- [ ] Full-screen map visible below top bar
- [ ] Map shows OpenSnowMap tiles (piste overlay visible if zoomed in enough)
- [ ] Blue START FAB centered horizontally at bottom (80dp margin)
- [ ] FAB is 120dp in size
- [ ] Tap menu: dropdown appears with Settings, About, Version Info

**2. Recording Start Transition**
- [ ] Tap START FAB
- [ ] Recording begins (timer starts)
- [ ] Top bar remains visible
- [ ] Map stays full-screen (does NOT shrink)
- [ ] Bottom sheet drawer slides up from bottom showing stats
- [ ] Bottom sheet has drag handle (thin bar) at top
- [ ] Stats panel is scrollable within bottom sheet
- [ ] Blue FAB replaced with RED FAB (floating over drawer)
- [ ] RED FAB centered at bottom

**3. During Recording**
- [ ] Map shows live GPS track (track points appear as polyline)
- [ ] Track polyline has speed-based coloring (green/yellow/red)
- [ ] Stats update in real-time in bottom sheet:
  - [ ] Current Speed (circular indicators)
  - [ ] Distance
  - [ ] Elevation
  - [ ] Max Speed
  - [ ] Other stats
- [ ] RED FAB displays pulse animation (1.0 → 1.15 scale, noticeable 1-second cycle)
- [ ] Can scroll stats within bottom sheet
- [ ] Can pan/zoom map even with drawer open
- [ ] Can drag bottom sheet up/down to expand/collapse
- [ ] Recording continues if bottom sheet is collapsed

**4. Stop Recording (Hold Detection)**
- [ ] Press and hold RED FAB for <4 seconds: nothing happens
- [ ] Release before 4s: FAB returns to normal pulse
- [ ] Press and hold RED FAB for ≥4 seconds:
  - [ ] Progress ring appears around FAB (yellow)
  - [ ] Progress ring fills as you hold
  - [ ] At 4s: progress ring turns green, recording stops
- [ ] After stop: Share options dialog appears
- [ ] Screen returns to idle state (full-screen map + blue FAB)

**5. Accessibility**
- [ ] All text readable (sufficient contrast)
- [ ] Touch targets ≥48dp (FABs are 120dp, icons are 48dp)
- [ ] GPS indicator updates smoothly
- [ ] No layout glitches when toggling portrait/landscape (if supported)

**6. Error States**
- [ ] If location permission denied:
  - [ ] Message displayed instead of map: "Location permission required..."
  - [ ] Tap START still works (requests permission)
- [ ] If GPS signal lost during recording:
  - [ ] Map doesn't crash
  - [ ] Stats still update
  - [ ] Recording continues

### Step 1: Deploy to test device

```bash
./gradlew installDebug
```

### Step 2: Execute test plan

Manually test each item, taking notes on any failures.

### Step 3: Fix any issues found

If tests fail, identify the issue and fix before committing.

### Step 4: Final build verification

```bash
./gradlew clean build -x test --quiet
```

Expected: Zero build errors.

### Step 5: Commit final verification

```bash
git add -A
git commit -m "test: Phase 1.3 implementation complete and verified"
```

---

## Task 9: Review and Handoff

**Files:** All Phase 1.3 changes

**Purpose:** Prepare for code review and completion.

### Step 1: Verify git log

```bash
git log --oneline -10
```

Expected output should show Phase 1.3 commits:
- "test: Phase 1.3 implementation complete and verified"
- "feat: auto-center map on first GPS fix"
- "feat: refactor RecordingScreen to map-centric layout with bottom sheet drawer for stats"
- "feat: create HoldToStopFab with 4-second hold detection and pulse animation"
- "feat: create reusable CircularFab component with tap feedback"
- "feat: add FAB and map state fields to RecordingUiState"
- "feat: add StartScreenTopBar with GPS indicator and menu dropdown"

### Step 2: Verify no uncommitted changes

```bash
git status
```

Expected: "nothing to commit, working tree clean"

### Step 3: Summary of changes

All Phase 1.3 requirements completed:
- ✅ Custom top bar with GPS signal indicator
- ✅ Menu dropdown (Settings, About, Version)
- ✅ Full-screen map (always visible, idle and recording)
- ✅ Circular blue START FAB (120dp)
- ✅ Bottom sheet drawer for recording stats (draggable, dismissible)
- ✅ Drag handle indicator on bottom sheet
- ✅ Circular RED STOP FAB (120dp, floating over drawer)
- ✅ 4-second hold-to-stop with progress ring
- ✅ Pulse animation (1.0 → 1.15, 1000ms)
- ✅ Live track polyline with speed coloring
- ✅ Auto-center on GPS location
- ✅ Permission handling and fallback UI

### Step 4: Files created/modified

**New files:**
- `app/src/main/java/com/skigpxrecorder/ui/components/StartScreenTopBar.kt`
- `app/src/main/java/com/skigpxrecorder/ui/components/CircularFab.kt`
- `app/src/main/java/com/skigpxrecorder/ui/components/HoldToStopFab.kt`
- `app/src/main/res/drawable/ic_signal_bar.xml`

**Modified files:**
- `app/src/main/java/com/skigpxrecorder/ui/RecordingScreen.kt`
- `app/src/main/java/com/skigpxrecorder/ui/RecordingViewModel.kt`
- `app/src/main/java/com/skigpxrecorder/data/repository/GpxRepository.kt`

### Step 5: Ready for completion

Phase 1.3 implementation is complete, tested, and ready for final code review via `superpowers:finishing-a-development-branch`.

