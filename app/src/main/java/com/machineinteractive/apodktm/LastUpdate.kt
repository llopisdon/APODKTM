package com.machineinteractive.apodktm

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LastUpdate(
    @PrimaryKey
    val id: String,
    val timetamp: Long
)