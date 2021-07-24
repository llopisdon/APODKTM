package com.machineinteractive.apodktm

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity
data class LastUpdate(
    @PrimaryKey
    val id: String,
    val timestamp: Long
)