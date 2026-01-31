package com.skigpxrecorder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.skigpxrecorder.data.model.RecordingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM recording_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): RecordingSession?

    @Query("SELECT * FROM recording_sessions WHERE isActive = 1 LIMIT 1")
    fun getActiveSessionFlow(): Flow<RecordingSession?>

    @Query("SELECT * FROM recording_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<RecordingSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: RecordingSession)

    @Update
    suspend fun updateSession(session: RecordingSession)

    @Query("UPDATE recording_sessions SET isActive = 0 WHERE id = :sessionId")
    suspend fun markSessionInactive(sessionId: String)

    @Query("DELETE FROM recording_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM recording_sessions WHERE isActive = 0")
    suspend fun deleteInactiveSessions()
}
