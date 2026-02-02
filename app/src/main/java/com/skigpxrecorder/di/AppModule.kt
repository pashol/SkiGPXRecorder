package com.skigpxrecorder.di

import android.content.Context
import com.skigpxrecorder.data.local.SessionDatabase
import com.skigpxrecorder.data.local.SessionDao
import com.skigpxrecorder.data.local.TrackPointDao
import com.skigpxrecorder.data.local.SkiRunDao
import com.skigpxrecorder.data.local.UserPreferences
import com.skigpxrecorder.domain.RunDetector
import com.skigpxrecorder.domain.SessionAnalyzer
import com.skigpxrecorder.service.LocationServiceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSessionDatabase(@ApplicationContext context: Context): SessionDatabase {
        return SessionDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: SessionDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    @Singleton
    fun provideTrackPointDao(database: SessionDatabase): TrackPointDao {
        return database.trackPointDao()
    }

    @Provides
    @Singleton
    fun provideSkiRunDao(database: SessionDatabase): SkiRunDao {
        return database.skiRunDao()
    }

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun provideRunDetector(): RunDetector {
        return RunDetector
    }

    @Provides
    @Singleton
    fun provideSessionAnalyzer(): SessionAnalyzer {
        return SessionAnalyzer
    }
}
