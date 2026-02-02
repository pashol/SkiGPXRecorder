package com.skigpxrecorder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.skigpxrecorder.data.model.RecordingSession
import com.skigpxrecorder.data.model.SkiRunEntity
import com.skigpxrecorder.data.model.TrackPointEntity
import com.skigpxrecorder.util.Constants

@Database(
    entities = [RecordingSession::class, TrackPointEntity::class, SkiRunEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SessionDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun skiRunDao(): SkiRunDao

    companion object {
        @Volatile
        private var INSTANCE: SessionDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to recording_sessions
                db.execSQL("ALTER TABLE recording_sessions ADD COLUMN endTime INTEGER")
                db.execSQL("ALTER TABLE recording_sessions ADD COLUMN finalFilePath TEXT")
                db.execSQL("ALTER TABLE recording_sessions ADD COLUMN sessionName TEXT")
                db.execSQL("ALTER TABLE recording_sessions ADD COLUMN runsCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE recording_sessions ADD COLUMN source TEXT NOT NULL DEFAULT 'RECORDED'")

                // Create track_points table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS track_points (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        elevation REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        accuracy REAL NOT NULL,
                        speed REAL NOT NULL,
                        heartRate REAL,
                        FOREIGN KEY(sessionId) REFERENCES recording_sessions(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_track_points_sessionId ON track_points(sessionId)")

                // Create ski_runs table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ski_runs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId TEXT NOT NULL,
                        runNumber INTEGER NOT NULL,
                        startIndex INTEGER NOT NULL,
                        endIndex INTEGER NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        startElevation REAL NOT NULL,
                        endElevation REAL NOT NULL,
                        maxSpeed REAL NOT NULL,
                        avgSpeed REAL NOT NULL,
                        distance REAL NOT NULL,
                        verticalDrop REAL NOT NULL,
                        avgSlope REAL NOT NULL,
                        pointCount INTEGER NOT NULL,
                        FOREIGN KEY(sessionId) REFERENCES recording_sessions(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ski_runs_sessionId ON ski_runs(sessionId)")
            }
        }

        fun getDatabase(context: Context): SessionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SessionDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
