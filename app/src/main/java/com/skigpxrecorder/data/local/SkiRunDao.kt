package com.skigpxrecorder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.skigpxrecorder.data.model.SkiRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkiRunDao {
    @Query("SELECT * FROM ski_runs WHERE sessionId = :sessionId ORDER BY runNumber ASC")
    suspend fun getRunsForSession(sessionId: String): List<SkiRunEntity>

    @Query("SELECT * FROM ski_runs WHERE sessionId = :sessionId ORDER BY runNumber ASC")
    fun getRunsForSessionFlow(sessionId: String): Flow<List<SkiRunEntity>>

    @Query("SELECT * FROM ski_runs WHERE sessionId = :sessionId AND runNumber = :runNumber LIMIT 1")
    suspend fun getRunByNumber(sessionId: String, runNumber: Int): SkiRunEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuns(runs: List<SkiRunEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: SkiRunEntity)

    @Query("DELETE FROM ski_runs WHERE sessionId = :sessionId")
    suspend fun deleteRunsForSession(sessionId: String)

    @Query("SELECT COUNT(*) FROM ski_runs WHERE sessionId = :sessionId")
    suspend fun getRunCount(sessionId: String): Int
}
