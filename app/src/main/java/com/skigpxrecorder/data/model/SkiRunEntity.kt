package com.skigpxrecorder.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisting detected ski runs
 */
@Entity(
    tableName = "ski_runs",
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
data class SkiRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
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
) {
    fun toSkiRun(): SkiRun {
        return SkiRun(
            runNumber = runNumber,
            startIndex = startIndex,
            endIndex = endIndex,
            startTime = startTime,
            endTime = endTime,
            startElevation = startElevation,
            endElevation = endElevation,
            maxSpeed = maxSpeed,
            avgSpeed = avgSpeed,
            distance = distance,
            verticalDrop = verticalDrop,
            avgSlope = avgSlope,
            pointCount = pointCount
        )
    }

    companion object {
        fun fromSkiRun(sessionId: String, run: SkiRun): SkiRunEntity {
            return SkiRunEntity(
                sessionId = sessionId,
                runNumber = run.runNumber,
                startIndex = run.startIndex,
                endIndex = run.endIndex,
                startTime = run.startTime,
                endTime = run.endTime,
                startElevation = run.startElevation,
                endElevation = run.endElevation,
                maxSpeed = run.maxSpeed,
                avgSpeed = run.avgSpeed,
                distance = run.distance,
                verticalDrop = run.verticalDrop,
                avgSlope = run.avgSlope,
                pointCount = run.pointCount
            )
        }
    }
}
