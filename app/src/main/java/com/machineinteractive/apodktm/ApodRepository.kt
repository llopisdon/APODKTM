package com.machineinteractive.apodktm

import android.util.Log
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.*

class ApodRepository(private val apodDao: ApodDao, private val client: HttpClient) {

    companion object {
        const val API_APOD_URL = "https://api.nasa.gov/planetary/apod"
    }

    suspend fun updateApods(date: LocalDate) {

        // note: the date.day value is ignored
        // see calcStartDayOfMonth for details

        //
        // fetch apods from today's date to start of month
        //

        val startDayOfMonth = calcStartDayOfMonthForDate(date)
        val startDate = LocalDate(date.year, date.monthNumber, startDayOfMonth)
        val lastDayOfMonth = calcLastDayOfMonthForDate(date)
        val endDate = LocalDate(date.year, date.monthNumber, lastDayOfMonth)

        Log.d(TAG, "updateApods - enddate: $endDate - startDate: $startDate")

        try {
            val apods: List<Apod> = client.get(API_APOD_URL) {
                parameter("api_key", "DEMO_KEY")
                parameter("start_date", startDate)
                parameter("end_date", endDate)
            }
            // only store APODs with images
            apodDao.insertApods(apods.filter { it.media_type == "image" })
            val id = "apod_${endDate.year}_${endDate.monthNumber}"
            val today = Clock.System.todayAt(TimeZone.currentSystemDefault())
            apodDao.setLastUpdate(LastUpdate(id, today))

        } catch (e: Exception) {
            Log.d(TAG, "refreshAPODs...failed: $e")
        }
    }

    suspend fun needsUpdate(date: LocalDate): Boolean {
        val id = "apod_${date.year}_${date.monthNumber}"
        val lastUpdate = apodDao.getLastUpdate(id) ?: return true
        val today = Clock.System.todayAt(TimeZone.currentSystemDefault())
        return lastUpdate.date < today
    }

    fun getApods(date: LocalDate): Flow<List<Apod>> {

        val lastDayOfMonth = calcLastDayOfMonthForDate(date)

        Log.d(TAG, "dom: $date - last day: $lastDayOfMonth")

        val startDayOfMonth = calcStartDayOfMonthForDate(date)

        val startDate = LocalDate(date.year, date.monthNumber, startDayOfMonth)
        val endDate = LocalDate(date.year, date.monthNumber, lastDayOfMonth)
        return apodDao.getApods(startDate, endDate)
    }

    private fun calcStartDayOfMonthForDate(date: LocalDate): Int {
        return if (date.year == APOD_EPOCH_YEAR && date.monthNumber == APOD_EPOCH_MONTH) {
            APOD_EPOCH_DAY
        } else {
            1
        }
    }

    private fun calcLastDayOfMonthForDate(date: LocalDate): Int {
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
