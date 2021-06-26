package com.machineinteractive.apodktm

import androidx.room.*
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class Apod(
    @PrimaryKey
    var date: String,
    var copyright: String? = null,
    var explanation: String? = null,
    var hdurl: String? = null,
    var media_type: String? = null,
    var service_version: String? = null,
    var title: String? = null,
    var url: String? = null
)




