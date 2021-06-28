package com.machineinteractive.apodktm

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ Apod::class, LastUpdate::class ], version = 1)
@TypeConverters(Converters::class)
abstract class ApodDatabase : RoomDatabase() {
    abstract fun apodDao(): ApodDao
}
