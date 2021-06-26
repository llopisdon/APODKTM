package com.machineinteractive.apodktm

import androidx.room.RoomDatabase

abstract class ApodDatabase : RoomDatabase() {
    abstract fun apodDao(): ApodDao
}
