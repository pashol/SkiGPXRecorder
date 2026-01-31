package com.skigpxrecorder.util

object Constants {
    // Location update interval (milliseconds)
    const val LOCATION_UPDATE_INTERVAL = 1000L // 1 second

    // Auto-save interval (milliseconds)
    const val AUTO_SAVE_INTERVAL = 60_000L // 60 seconds

    // GPS accuracy threshold (meters)
    const val GPS_ACCURACY_THRESHOLD = 30f

    // Elevation change threshold (meters) - ignore changes smaller than this
    const val ELEVATION_CHANGE_THRESHOLD = 3f

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

    // FileProvider authority
    const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"

    // Database
    const val DATABASE_NAME = "session_database"

    // Storage
    const val GPX_CACHE_DIR = "gpx"
    const val DOWNLOADS_SUBDIR = "SkiGPX"
}
