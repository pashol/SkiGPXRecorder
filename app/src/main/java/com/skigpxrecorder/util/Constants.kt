package com.skigpxrecorder.util

object Constants {
    // Location update interval (milliseconds)
    const val LOCATION_UPDATE_INTERVAL = 1000L // 1 second

    // Auto-save interval (milliseconds)
    const val AUTO_SAVE_INTERVAL = 60_000L // 60 seconds

    // GPS accuracy threshold (meters)
    const val GPS_ACCURACY_THRESHOLD = 30f

    // Battery thresholds
    const val BATTERY_WARNING_THRESHOLD = 10 // 10%
    const val BATTERY_AUTO_STOP_THRESHOLD = 5 // 5%

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "ski_gpx_recording_channel"
    const val NOTIFICATION_ID = 1001

    // Service
    const val SERVICE_ACTION_START = "ACTION_START_RECORDING"
    const val SERVICE_ACTION_STOP = "ACTION_STOP_RECORDING"

    // GPX Export
    const val GPX_MIME_TYPE = "application/gpx+xml"
    const val GPX_FILE_PREFIX = "Ski_Track_"
    const val GPX_FILE_EXTENSION = ".gpx"
    const val TEMP_GPX_FILE_NAME = "temp_recording.gpx"

    // FIT Import
    const val FIT_MIME_TYPE = "application/vnd.ant.fit"

    // FileProvider authority
    const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"

    // Database
    const val DATABASE_NAME = "session_database"

    // Storage
    const val GPX_CACHE_DIR = "gpx"
    const val DOWNLOADS_SUBDIR = "SkiGPX"

    // Run Detection - Window-based algorithm (matches Ski-GPX-Analyzer)
    const val RUN_DETECTION_SPEED_THRESHOLD = 5.0f // km/h - minimum speed to be considered moving
    const val RUN_DETECTION_MIN_DURATION = 60 // seconds - minimum run duration
    const val RUN_DETECTION_MIN_VERTICAL = 30f // meters - minimum vertical drop
    const val RUN_DETECTION_TREND_WINDOW = 20 // points - window size for trend-based detection
    const val RUN_DETECTION_MIN_WINDOW_DROP = 10f // meters - minimum elevation drop in window
    const val RUN_DETECTION_MAX_GAP_TIME = 120 // seconds - maximum gap time to combine segments
    const val RUN_DETECTION_MAX_ASCENT_IN_GAP = 50f // meters - maximum ascent in gap to combine segments
    const val SPEED_SMOOTHING_WINDOW = 5 // points - window size for speed smoothing
}
