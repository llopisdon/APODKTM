package com.machineinteractive.apodktm

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ Apod::class, LastUpdate::class ], version = 1)
abstract class ApodDatabase : RoomDatabase() {
    abstract fun apodDao(): ApodDao
}
