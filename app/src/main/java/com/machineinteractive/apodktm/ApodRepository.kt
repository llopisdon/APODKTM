package com.machineinteractive.apodktm

import android.util.Log
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.utils.io.concurrent.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.*

class ApodRepository(private val apodDao: ApodDao, private val client: HttpClient) {

    fun getApods(startDate: LocalDate, endDate: LocalDate): Flow<List<Apod>> {
        return apodDao.getAll()
    }

    fun getApods(): Flow<List<Apod>> {
        return apodDao.getAll()
    }

    companion object {
        const val API_APOD_URL = "https://api.nasa.gov/planetary/apod"
    }

    suspend fun updateApods(endDate: LocalDate) {

        val today = Clock.System.todayAt(TimeZone.currentSystemDefault())

        val prevPeriod = endDate < today

        Log.d(TAG, "prevPeriod: $prevPeriod")

        // TODO
        // fetch apods from today's date to start of month

        val firstDayOfMonth = LocalDate(endDate.year, endDate.monthNumber, 1)

        Log.d(TAG, "updateApods - enddate: $endDate - firsday: $firstDayOfMonth")

        try {
            val apods: List<Apod> = client.get(API_APOD_URL) {
                parameter("api_key", "DEMO_KEY")
                parameter("start_date", firstDayOfMonth)
                parameter("end_date", endDate)
            }

            // only store APODs with images
            apodDao.insertApods(apods.filter { it.media_type == "image" })
            val id = "apod_${endDate.year}_${endDate.monthNumber}"
            apodDao.setLastUpdate(LastUpdate(id, today))

        } catch (e: Exception) {
            Log.d(TAG, "refreshAPODs...failed: $e")
        }
    }

    suspend fun needsUpdate(fromDate: LocalDate): Boolean {
        val id = "apod_${fromDate.year}_${fromDate.monthNumber}"
        val lastUpdate = apodDao.getLastUpdate(id) ?: return true
        val today = Clock.System.todayAt(TimeZone.currentSystemDefault())
        return lastUpdate.date < today
    }

    fun getApods(endDate: LocalDate): Flow<List<Apod>> {
        val startDate = LocalDate(endDate.year, endDate.monthNumber, 1)
        return apodDao.getApods(startDate, endDate)
    }
}
