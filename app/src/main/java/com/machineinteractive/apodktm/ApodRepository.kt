package com.machineinteractive.apodktm

import android.util.Log
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.ConnectException

sealed class ApodResult {
    object Success : ApodResult()
    class Error(val code: String, val message: String?) : ApodResult()
}

class ApodRepository(private val apodDao: ApodDao) {

    companion object {
        const val API_APOD_URL = "https://api.nasa.gov/planetary/apod"
    }

    private fun getStartEndRangeForDate(date: LocalDate): Pair<LocalDate, LocalDate> {
        val lastDayOfMonth = getLastDayOfMonthForDate(date)
        Log.d(TAG, "calcStartEndDates - dom: $date | last day: $lastDayOfMonth")
        val startDayOfMonth = getFirstDayOfMonthForDate(date)
        val startDate = LocalDate(date.year, date.monthNumber, startDayOfMonth)
        val endDate = LocalDate(date.year, date.monthNumber, lastDayOfMonth)
        return Pair(startDate, endDate)
    }

    fun getApodsForCurMonth(date: LocalDate): Flow<List<Apod>> {
        val (startDate, endDate) = getStartEndRangeForDate(date)
        return apodDao.getApods(startDate, endDate)
    }

    suspend fun curMonthHasApods(date: LocalDate): Boolean {
        val (startDate, endDate) = getStartEndRangeForDate(date)
        return apodDao.getApodsCount(startDate, endDate) > 0
    }

    suspend fun updateApodsForCurMonth(date: LocalDate): ApodResult {

        // note: the date.day value is ignored
        // see calcStartDayOfMonth for details

        //
        // fetch apods from today's date to start of month
        //

        val firstDayOfMonth = getFirstDayOfMonthForDate(date)
        val lastDayOfMonth = getLastDayOfMonthForDate(date)
        val startDate = LocalDate(date.year, date.monthNumber, firstDayOfMonth)
        val endDate = LocalDate(date.year, date.monthNumber, lastDayOfMonth)

        Log.d(TAG, "updateApodsForCurMonth\n\tstartDate: $startDate | enddate: $endDate")
        val (apods, result) = fetchApods(startDate, endDate)

        apodDao.insertApods(apods)

        val id = getLastUpdateId(endDate)
        val timestamp = if (apods.isEmpty()) {
            0L
        } else {
            Clock.System.now().toEpochMilliseconds()
        }

        Log.d(
            TAG,
            "\tHAS APODS: ${apods.isEmpty()} | set last update - id: $id timestamp: ${
                Instant.fromEpochMilliseconds(timestamp)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            }"
        )

        apodDao.setLastUpdate(LastUpdate(id, timestamp))
        return result
    }

    // note: sometimes requesting thumbnails can cause the server to crash with a 500
    // this has been reported by several people. need to check for 500 then try
    // a second request without thumbs to work around this.
    private suspend fun fetchApods(
        startDate: LocalDate,
        endDate: LocalDate
    ): Pair<List<Apod>, ApodResult> {

        var thumbnailsFailed = false

        try {
            Log.d(TAG, "fetchApods - THUMBS")
            return fetchApods(startDate, endDate, true)
        } catch (e: Exception) {
            when (e) {
                is ServerResponseException -> {
                    if (e.response.status == HttpStatusCode.InternalServerError) {
                        Log.d(TAG, "ServerResponseException:\n${e.response.readText()}")
                        thumbnailsFailed = true
                    }
                }
                else -> {
                    Log.d(TAG, "exception: $e")
                    e.printStackTrace()
                    val (code, errorMessage) = parseException(e)
                    return Pair(emptyList(), ApodResult.Error(code, errorMessage))
                }
            }
        }

        if (thumbnailsFailed) {
            Log.d(TAG, "fetchApods - w/o THUMBS")
            try {
                return fetchApods(startDate, endDate, false)
            } catch (e: Exception) {
                Log.d(TAG, "exception: $e")
                e.printStackTrace()
                val (code, errorMessage) = parseException(e)
                return Pair(emptyList(), ApodResult.Error(code, errorMessage))
            }
        }

        return Pair(emptyList(), ApodResult.Success)
    }


    private suspend fun fetchApods(
        startDate: LocalDate,
        endDate: LocalDate,
        thumbs: Boolean
    ): Pair<List<Apod>, ApodResult> {
        return getHttpClient().use { client ->
            val apods: List<Apod> = client.get(API_APOD_URL) {
                parameter("api_key", BuildConfig.APOD_API_KEY)
                parameter("start_date", startDate)
                parameter("end_date", endDate)
                parameter("thumbs", thumbs)
            }
            Pair(apods, ApodResult.Success)
        }
    }


    private suspend fun parseException(e: Exception): Pair<String, String> {
        return when (e) {
            is ClientRequestException -> {
                val errorBody: String = (e as ResponseException).response.readText()
                val response = Json.decodeFromString<ApodApiErrorResponse>(errorBody)
                Log.d(TAG, "$response")
                Pair(
                    CLIENT_REQUEST_ERROR,
                    "Api Error: ${response.error.code} - ${response.error.message}"
                )
            }
            is ServerResponseException -> {
                val errorBody: String = (e as ResponseException).response.readText()
                val response = Json.decodeFromString<ApodServerErrorResponse>(errorBody)
                Log.d(TAG, "$response")
                Pair(
                    SERVER_ERROR,
                    "Server Error: ${response.code} - ${response.msg} - ${response.service_version}"
                )
            }
            is IOException,
            is UnresolvedAddressException,
            is ConnectException -> {
                Pair(
                    NETWORK_ERROR,
                    "Unable to connect to server. Please check your network settings."
                )
            }
            else -> {
                val message = e.message ?: "An unknown error occurred."
                Pair(UNKNOWN_ERROR, message)
            }
        }
    }

    private fun getLastUpdateId(date: LocalDate): String =
        "apod_${date.year}_${date.monthNumber}"

    suspend fun needsUpdate(date: LocalDate): Boolean {

        val id = getLastUpdateId(date)
        Log.d(TAG, "needsUpdate - $date")

        //
        // first check to see if we have any updates for the passed in month
        // no regardless of the date
        //

        val lastUpdate = apodDao.getLastUpdate(id)
        if (lastUpdate == null || lastUpdate.timestamp == 0L) {
            Log.d(TAG, "needsUpdate: true -> lastUpdate is NULL or 0L")
            return true
        }

        //
        // check if the month is in the past if so then try one final update
        // before giving up
        //
        // if the date is not for the current month
        // then check to see if we have fetched all APODs for the date passed in
        // so if we have no apod for the last day of month and the
        // last update was before the following month then
        // try to refresh the apods one more time before giving up
        //

        val today = Clock.System.todayAt(TimeZone.currentSystemDefault())
        if (today.month != date.month || today.year != date.year) {

            val lastDayOfMonthForDate = LocalDate(
                date.year,
                date.monthNumber,
                getLastDayOfMonthForDate(date)
            )

            val lastApod = apodDao.getApodForDay(lastDayOfMonthForDate)

            val nextMonthForDate = lastDayOfMonthForDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(
                TimeZone.currentSystemDefault())
            val timeStampInstant = Instant.fromEpochMilliseconds(lastUpdate.timestamp)
            val secondsUntil = timeStampInstant.until(nextMonthForDate, DateTimeUnit.SECOND, TimeZone.currentSystemDefault())

            if (secondsUntil > 0 && lastApod == null) {
                Log.d(TAG, "needsUpdate: true - sec: $secondsUntil | PREV MONTH NEEDS ONE LAST CHECK FOR APODS")
                return true
            } else {
                Log.d(TAG, "needsUpdate: false - PREV MONTH UP-TO-DATE")
                return false
            }
        }

        //
        // check if the current date has an APOD
        //

        val apod = apodDao.getApodForDay(date)
        if (apod != null) {
            Log.d(TAG, "needsUpdate: false -> has APOD for current day ! ")
            return false
        }

        //
        // current date has no APOD (yet) try checking every hour
        //

        val instant = Instant.fromEpochMilliseconds(lastUpdate.timestamp)
        val diffInHours = instant.until(
            Clock.System.now(),
            DateTimeUnit.HOUR,
            TimeZone.currentSystemDefault()
        )

        Log.d(TAG, "needsUpdate: ${diffInHours > 1} -> diff hours: $diffInHours")

        return diffInHours > 1
    }

    private fun getFirstDayOfMonthForDate(date: LocalDate): Int {
        // we need to check for the APOD Epoch because sending a start day
        // earlier than that will result in a server error
        return if (date.year == APOD_EPOCH_YEAR && date.monthNumber == APOD_EPOCH_MONTH) {
            APOD_EPOCH_DAY
        } else {
            1
        }
    }

    private fun getLastDayOfMonthForDate(date: LocalDate): Int {
        val today = Clock.System.todayAt(TimeZone.currentSystemDefault())
        return if (today.year == date.year && today.monthNumber == date.monthNumber) {
            today.dayOfMonth
        } else {
            LocalDate(date.year, date.monthNumber, 1)
                .plus(1, DateTimeUnit.MONTH)
                .minus(1, DateTimeUnit.DAY).dayOfMonth
        }
    }
}



