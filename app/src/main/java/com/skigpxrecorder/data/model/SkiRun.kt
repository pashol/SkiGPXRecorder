package com.skigpxrecorder.data.model

/**
 * Represents a detected ski run (downhill segment)
 */
data class SkiRun(
    val runNumber: Int,
    val startIndex: Int,
    val endIndex: Int,
    val startTime: Long,
    val endTime: Long,
    val startElevation: Double,
    val endElevation: Double,
    val maxSpeed: Float,
    val avgSpeed: Float,
    val distance: Float,
    val verticalDrop: Float,
    val avgSlope: Float,
    val pointCount: Int
)
