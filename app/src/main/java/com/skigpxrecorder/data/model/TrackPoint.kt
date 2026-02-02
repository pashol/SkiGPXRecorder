package com.skigpxrecorder.data.model

import android.location.Location
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Represents a single GPS track point
 */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val timestamp: Long, // epoch milliseconds
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val heartRate: Float? = null
) {
    companion object {
        fun fromLocation(location: Location): TrackPoint {
            return TrackPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                elevation = location.altitude,
                timestamp = location.time,
                accuracy = location.accuracy,
                speed = location.speed * 3.6f // Convert m/s to km/h
            )
        }
    }

    /**
     * Convert timestamp to ISO 8601 format for GPX
     */
    fun getIsoTimestamp(): String {
        val instant = Instant.ofEpochMilli(timestamp)
        return DateTimeFormatter.ISO_INSTANT.format(instant)
    }
}

/**
 * Room entity for crash recovery session state
 */
@Entity(tableName = "recording_sessions")
data class RecordingSession(
    @PrimaryKey
    val id: String,
    val startTime: Long,
    val endTime: Long? = null,
    val isActive: Boolean = true,
    val lastSavedPointIndex: Int = 0,
    val tempFilePath: String? = null,
    val finalFilePath: String? = null,
    val sessionName: String? = null,
    val pointCount: Int = 0,
    val distance: Float = 0f,
    val elevationGain: Float = 0f,
    val elevationLoss: Float = 0f,
    val maxSpeed: Float = 0f,
    val runsCount: Int = 0,
    val source: String = "RECORDED" // DataSource enum as string
)
