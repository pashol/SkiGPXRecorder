package com.skigpxrecorder.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisting track points
 */
@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val timestamp: Long,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val heartRate: Float? = null
) {
    fun toTrackPoint(): TrackPoint {
        return TrackPoint(
            latitude = latitude,
            longitude = longitude,
            elevation = elevation,
            timestamp = timestamp,
            accuracy = accuracy,
            speed = speed,
            heartRate = heartRate
        )
    }

    companion object {
        fun fromTrackPoint(sessionId: String, point: TrackPoint): TrackPointEntity {
            return TrackPointEntity(
                sessionId = sessionId,
                latitude = point.latitude,
                longitude = point.longitude,
                elevation = point.elevation,
                timestamp = point.timestamp,
                accuracy = point.accuracy,
                speed = point.speed,
                heartRate = point.heartRate
            )
        }
    }
}
