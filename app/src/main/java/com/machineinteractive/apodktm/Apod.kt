package com.machineinteractive.apodktm

import androidx.room.*
import kotlinx.datetime.*
import kotlinx.serialization.Serializable


class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let {
            Instant
                .fromEpochMilliseconds(it)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        }
    }
    @TypeConverter
    fun localDateToTimestamp(date: LocalDate?): Long? {
        val tz = TimeZone.currentSystemDefault()
        return date?.atStartOfDayIn(tz)?.toEpochMilliseconds()
    }
}


@Serializable
@Entity
data class Apod(
    @PrimaryKey
    var date: LocalDate,
    var copyright: String? = null,
    var explanation: String? = null,
    var hdurl: String? = null,
    var media_type: String? = null,
    var service_version: String? = null,
    var title: String? = null,
    var url: String? = null,
    var thumbnail_url: String? = null
)

@Serializable
data class ApodApiErrorResponse(
    val error: ApodApiError
)

@Serializable
data class ApodApiError(
    val code: String,
    var message: String
)

@Serializable
data class ApodServerErrorResponse(
    val code: Int,
    val msg: String,
    val service_version: String
)




