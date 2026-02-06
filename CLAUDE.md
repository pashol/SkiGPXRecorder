# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SkiGPXRecorder is an Android application that records GPS tracks during skiing activities and exports them as GPX files. The app uses a foreground service for continuous location tracking and implements battery-aware auto-save functionality.

## Build and Development Commands

### Building the App
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build to connected device/emulator
./gradlew installDebug
```

### Running Tests
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests com.skigpxrecorder.YourTestClass
```

### Code Quality
```bash
# Clean build artifacts
./gradlew clean

# Check for dependency updates
./gradlew dependencyUpdates
```

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

### Important Constants
All thresholds and intervals are centralized in `util/Constants.kt`:
- `LOCATION_UPDATE_INTERVAL`: GPS update frequency
- `GPS_ACCURACY_THRESHOLD`: Reference value for UI display (no longer used for filtering)
- `AUTO_SAVE_INTERVAL`: How often to save temporary GPX files
- `BATTERY_WARNING_THRESHOLD` / `BATTERY_AUTO_STOP_THRESHOLD`: Battery management levels

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

## Map Implementations

- **Recording screen** (`RecordingScreen.kt`): Google Maps Compose (`GoogleMapsView.kt`) for live tracking with smooth polyline overlay
- **Session playback screens** (e.g., `SessionScreen.kt`): osmdroid for historical track display (separate from recording flow)
- Both libraries coexist; gradual migration from osmdroid to Google Maps is planned

## Known Considerations

- The app uses `kapt` for Room and Hilt annotation processing, which requires special JVM flags (configured in gradle.properties)
- GPX files are saved to app-specific external storage using scoped storage (no WRITE_EXTERNAL_STORAGE permission needed on API 29+)
- The service uses PARTIAL_WAKE_LOCK without timeout - ensure proper cleanup on service destruction
- Battery monitoring uses BroadcastReceiver for ACTION_BATTERY_CHANGED
- GPS accuracy filtering is disabled - all positions are recorded with accuracy metadata in GPX extensions for post-processing
- `BottomSheetScaffold` uses `@ExperimentalMaterial3Api` - API may change in future Material3 releases
- RecordingScreen uses Box layering for map overlay effects (top bar gradient, floating button, bottom sheet)
