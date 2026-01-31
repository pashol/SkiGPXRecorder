# Ski GPX Recorder

An Android application for recording GPS tracks during skiing activities and exporting them as GPX files.

## Features

- **GPS Track Recording**: High-accuracy location tracking optimized for skiing
- **Foreground Service**: Continuous tracking even when app is in background
- **Battery Management**: Automatic warnings and session stop at low battery levels
- **Auto-Save**: Periodic saving to prevent data loss
- **GPX Export**: Standard GPX 1.1 format compatible with popular mapping tools
- **Real-time Statistics**: Speed, elevation, distance, and duration tracking
- **GPS Accuracy Filtering**: Only records points that meet accuracy threshold

## Requirements

- Android 10 (API 29) or higher
- Location permissions
- GPS-enabled device

## Permissions

The app requires the following permissions:
- **Location Access**: For GPS tracking during ski sessions
- **Foreground Service**: To continue tracking in background
- **Notifications**: To display recording status

## Technology

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material3
- **Architecture**: MVVM + Repository pattern
- **Dependency Injection**: Hilt/Dagger
- **Database**: Room
- **Location**: Google Play Services Fused Location Provider

## Building

```bash
# Build debug APK
./gradlew assembleDebug

# Install to device
./gradlew installDebug

# Run tests
./gradlew test
```

## Usage

1. Grant location permissions when prompted
2. Tap "Start Recording" to begin GPS tracking
3. The app will run in the background with a notification
4. View real-time statistics during recording
5. Tap "Stop Recording" to end session
6. GPX file is automatically saved and can be shared

## GPX Export

Recorded tracks are saved in GPX 1.1 format with:
- Track points with latitude/longitude coordinates
- Elevation data
- Timestamps in ISO 8601 format
- Automatic track naming with date/time

Files are saved to app-specific storage and can be shared with other applications.

## License

This project is open source. See LICENSE file for details.
