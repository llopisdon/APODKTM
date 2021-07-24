package com.machineinteractive.apodktm

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): ApodDatabase {
        return Room.databaseBuilder(
            appContext,
            ApodDatabase::class.java,
            "apod.db"
        ).build()
    }

    @Provides
    fun provideApodDao(database: ApodDatabase): ApodDao {
        return database.apodDao()
    }

    @Provides
    fun provideApodRepository(apodDao: ApodDao): ApodRepository {
        return ApodRepository(apodDao)
    }
}