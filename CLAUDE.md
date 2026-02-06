# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SkiGPXRecorder is an Android application that records GPS tracks during skiing activities, detects individual ski runs via descent analysis, exports tracks as GPX files, and provides comprehensive post-session analysis with maps, charts, and performance statistics. The app uses a foreground service for continuous location tracking, implements battery-aware auto-save, and features a multi-screen UI (recording, session playback with tabs, run detail views).

## Build and Development Commands

### Building
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug to device/emulator
./gradlew installDebug

# Build and launch immediately
./gradlew installDebug && adb shell am start -n com.skigpxrecorder/.MainActivity
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run instrumented Android tests (device/emulator required)
./gradlew connectedAndroidTest

# Run specific unit test class
./gradlew test --tests com.skigpxrecorder.domain.RunDetectorTest

# Run specific instrumented test
./gradlew connectedAndroidTest --tests com.skigpxrecorder.RecordingScreenTest
```

### Development Tools
```bash
# Clean build artifacts
./gradlew clean

# Inspect dependency tree
./gradlew dependencies
```

### Configuration
- **Google Maps API Key**: Create `secrets.properties` in project root with `MAPS_API_KEY=AIza...` (gitignored; see "Google Maps Setup" below)
- **Local overrides**: `local.properties` for SDK path, not committed

## Architecture

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt/Dagger
- **Database**: Room (for session persistence)
- **Location Services**: Google Play Services FusedLocationProviderClient
- **Maps**: Google Maps SDK for Android with maps-compose (recording screen), osmdroid (session playback)
- **Min SDK**: 29 (Android 10), Target SDK: 34

### Key Components

**LocationService** (Foreground Service)
- Core GPS tracking service that runs in the foreground
- Emits location updates via SharedFlow to UI layer
- Implements battery monitoring with auto-stop at low battery
- Uses WakeLock to prevent sleep during recording
- Auto-saves GPX data periodically (see Constants.AUTO_SAVE_INTERVAL)
- Records all GPS positions with accuracy metadata for post-processing filtering

**GpxRepository**
- Manages in-memory track points and database sessions
- Handles GPX file generation and saving to external storage
- Provides statistics calculation (distance, speed, elevation)
- Implements temporary save functionality for crash recovery
- Exports accuracy data in GPX extensions for each track point

**LocationServiceManager**
- Mediates between UI (ViewModel) and LocationService
- Handles service binding/unbinding lifecycle
- Provides reactive access to service state via StateFlow

**RecordingViewModel**
- Manages UI state and collects location updates from service
- Exposes recording state, statistics, and GPS data to UI
- Handles permission requests and service lifecycle

**GoogleMapsView** (UI Component)
- Full-screen Google Maps for recording screen (always visible)
- Displays track polyline overlay and current location blue dot
- Implements camera auto-follow to latest track point
- Uses maps-compose for native Jetpack Compose integration

**RecordingScreen** (UI Layer)
- Redesigned with full-screen map + bottom sheet + floating button
- Idle state: map + floating "Start Recording" button
- Recording state: map + semi-transparent top bar + BottomSheetScaffold
  - Sheet peek height (140.dp): 3 key stats + hold-to-stop button
  - Expanded: full stats display with StatCircles + detailed metrics
- Post-recording: animated share/save overlay

### Data Flow
1. User initiates recording in RecordingScreen
2. RecordingViewModel starts LocationService via LocationServiceManager
3. LocationService requests high-accuracy location updates from FusedLocationProviderClient
4. All location updates are converted to TrackPoints (with accuracy metadata) and emitted via SharedFlow
5. Repository stores points in memory and persists to Room database
6. ViewModel collects updates and exposes them to UI
7. Auto-save periodically writes temporary GPX files with accuracy extensions
8. On session end, final GPX file is generated and saved to external storage with accuracy data
9. Users can filter waypoints by accuracy during post-processing

### Ski Run Detection (RunDetector)
The app automatically detects ski runs using a window-based descent analysis algorithm:
- **20-point trend window**: Analyzes elevation change over recent points
- **5-point elevation smoothing**: Reduces noise in elevation data
- **Speed threshold**: Minimum 5 km/h to qualify as active skiing
- **Duration requirement**: Minimum 60 seconds per run
- **Vertical requirement**: Minimum 30m drop per run
- **Gap tolerance**: Runs separated by <120s and <50m ascent are combined
- All detected runs stored in Room database and accessible via `SkiRun` data model

### Statistics Calculation (StatsCalculator)
- **Distance**: Haversine formula between consecutive GPS points
- **Elevation change**: Cumulative gain/loss (no dead-zone filtering applied)
- **Speed**: Raw speed from GPS, smoothed via moving window (Constants.SPEED_SMOOTHING_WINDOW)
- Both batch and incremental (single-point) update modes available
- Critical for real-time UI updates and post-session analysis

### Important Constants
Centralized in `util/Constants.kt`:
- `LOCATION_UPDATE_INTERVAL`: 1000ms (GPS update frequency)
- `AUTO_SAVE_INTERVAL`: 60000ms (temp GPX file save period)
- `BATTERY_WARNING_THRESHOLD`: 10% (user warning level)
- `BATTERY_AUTO_STOP_THRESHOLD`: 5% (automatic session stop)
- `GPS_ACCURACY_THRESHOLD`: 30m (reference value; not used for filtering)
- `RUN_DETECTION_SPEED_THRESHOLD`: 5.0 km/h
- `RUN_DETECTION_MIN_DURATION`: 60 seconds
- `RUN_DETECTION_MIN_VERTICAL`: 30 meters
- `SPEED_SMOOTHING_WINDOW`: Window size for speed averaging

## Permissions and Services

The app requires runtime permissions for:
- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`
- `POST_NOTIFICATIONS` (Android 13+)
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_LOCATION`

LocationService runs as a foreground service with `foregroundServiceType="location"` to enable background GPS tracking during ski sessions.

## File Structure Notes

- `data/local/`: Room database entities and DAOs for session persistence
- `data/model/`: Data models (TrackPoint, RecordingSession)
- `data/repository/`: Repository layer managing data flow
- `domain/`: Business logic (GPX generation, statistics calculation)
- `service/`: LocationService and LocationServiceManager
- `ui/`: Composable screens and ViewModels
- `di/`: Hilt dependency injection modules

## Google Maps Setup (Recording Screen)

The recording screen uses Google Maps Compose for a modern, full-screen map experience. To use the app:

1. Obtain a Google Maps API key:
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create project and enable "Maps SDK for Android"
   - Create API key and restrict to package `com.skigpxrecorder`
2. Add key to `secrets.properties` (gitignored):
   ```
   MAPS_API_KEY=AIza...
   ```
3. A `local.defaults.properties` file (committed) provides a fallback placeholder

The secrets-gradle-plugin automatically injects the API key as a manifest meta-data value.

## UI Architecture & Navigation

### Screen Hierarchy
1. **RecordingScreen** - Main recording interface (Google Maps)
   - Recording: full-screen map + BottomSheetScaffold with stats + floating stop button
   - Idle: full-screen map + floating start button

2. **SessionScreen** - Playback & analysis (tabbed interface)
   - **TrackView**: Run list (SkiRunCard), session stats, jump to RunDetailScreen
   - **MapView**: osmdroid map with speed-colored polyline, markers, piste overlay
   - **ProfileView**: Large elevation chart with zoom, speed overlay, run region highlighting
   - **AnalysisView**: Performance metrics, speed histogram, time distribution

3. **RunDetailScreen** - Single run detail (NEW)
   - Fixed 300dp osmdroid RunMapView at top (speed-colored polyline, start/end markers, synced chart position)
   - Scrollable content below: stats grid, ElevationSpeedChart with interactive tooltip, SpeedHistogram, RunComparisonCard

4. **SessionHistoryScreen** - Session list with grouping/filtering
5. **SettingsScreen**, **HighscoreScreen**, **MapAllSessionsScreen**

### Map Implementations

- **Recording screen** (`RecordingScreen.kt`): **Google Maps Compose** (GoogleMapsView.kt) for live tracking with polyline overlay
- **Session map tab** (`SessionScreen.kt` → MapView.kt): **osmdroid** for historical track display with speed-colored polyline, markers, OpenSnowMap piste overlay
- **Run detail** (`RunDetailScreen.kt` → RunMapView.kt): **osmdroid** with speed-colored segments, start/end markers, synced position indicator, piste overlay (on by default)
- Both libraries coexist for different use cases; gradual consolidation planned

### Chart Components with Interactive Features
- **ElevationSpeedChart**: Dual-axis elevation + speed overlay with drag gesture support, floating tooltip (elevation/speed/distance), indicator circles, onPointSelected callback
- **ElevationProfileChart**: Zoomable elevation profile with optional speed overlay and run region highlighting
- **SpeedHistogram**: 6-bucket speed distribution bar chart
- **DonutChart**: Circular percentage display

## Key Implementation Notes

### Annotation Processing
- **KAPT**: Room and Hilt require special JVM flags (`-XX:+IgnoreUnrecognizedVMOptions`, `-XX:MaxPermSize=2048m`) in gradle.properties
- **Hilt**: All ViewModels use `@HiltViewModel` for automatic injection; Activities use `@AndroidEntryPoint`

### Data Persistence
- **Room Database**: 3 main tables (recording_sessions, track_points, ski_runs) with migration path v1→v2→v3
- **GPX Export**: Saved to app-specific external storage via scoped storage (API 29+, no WRITE_EXTERNAL_STORAGE needed)
- **Accuracy metadata**: All track points include GPS accuracy as GPX extensions for post-processing filtering

### Service Lifecycle
- **LocationService**: PARTIAL_WAKE_LOCK without timeout - ensure cleanup on service destruction
- **Battery monitoring**: BroadcastReceiver listening to ACTION_BATTERY_CHANGED (10% warning, 5% auto-stop)
- **Auto-save**: Periodic temp GPX writes every 60 seconds to prevent data loss on crash

### UI Specifics
- **Experimental APIs**: BottomSheetScaffold uses `@ExperimentalMaterial3Api`
- **Compose layering**: RecordingScreen uses Box stacking for map + top gradient bar + floating button + bottom sheet
- **Chart rendering**: Custom Canvas-based implementation (no external charting library); drawTooltip() renders floating info boxes
- **runMapView**: Uses custom Drawable classes for colored circle markers with text labels

### File Import/Export
- **Supported formats**: GPX (native) and FIT (Garmin)
- **Cloud providers**: Auto-detects and resolves cloud storage URIs
- **Error handling**: Retry logic with 500ms delays for network/IO operations
- **Database integration**: Imported sessions automatically persisted with run detection applied
