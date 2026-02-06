package com.skigpxrecorder.data.model

/**
 * Comprehensive statistics for a ski session
 */
data class SessionStats(
    // Distance metrics
    val totalDistance: Float = 0f,
    val skiDistance: Float = 0f,
    val liftDistance: Float = 0f,

    // Elevation metrics
    val skiVertical: Float = 0f,
    val totalAscent: Float = 0f,
    val totalDescent: Float = 0f,
    val maxAltitude: Float = 0f,
    val minAltitude: Float = 0f,

    // Speed metrics
    val maxSpeed: Float = 0f,
    val avgSpeed: Float = 0f,
    val avgSkiSpeed: Float = 0f,

    // Time distribution (seconds)
    val totalDuration: Long = 0,
    val movingTime: Long = 0,
    val stationaryTime: Long = 0,
    val ascendingTime: Long = 0,
    val descendingTime: Long = 0,

    // Heart rate (if available)
    val avgHeartRate: Float? = null,
    val maxHeartRate: Float? = null,

    // Performance
    val performanceScore: Float = 0f, // 0-100

    // Speed distribution buckets (counts for histogram)
    val speedDistribution: List<Int> = emptyList()
)
