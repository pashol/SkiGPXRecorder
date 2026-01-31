package com.skigpxrecorder.di

import android.content.Context
import com.skigpxrecorder.data.local.SessionDatabase
import com.skigpxrecorder.data.local.SessionDao
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
}
