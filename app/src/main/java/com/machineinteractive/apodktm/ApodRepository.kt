package com.machineinteractive.apodktm

import android.util.Log
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.*

class ApodRepository(private val apodDao: ApodDao, private val client: HttpClient) {

    fun getApods(): Flow<List<Apod>> {
        return apodDao.getAll()
    }

    companion object {
        const val API_APOD_URL = "https://api.nasa.gov/planetary/apod"
    }

    suspend fun updateApods() {
        // TODO
        // fetch apods from today's date to start of month

        val today = Clock.System.todayAt(TimeZone.currentSystemDefault())

        Log.d(TAG, "updateApods: $today")

        try {
            Log.d(TAG, "refreshAPODs...delay 10s")
            delay(10_000L)
            Log.d(TAG, "refreshAPODs...fetching...")
            val apods: List<Apod> = client.get(API_APOD_URL) {
                parameter("api_key", "DEMO_KEY")
                parameter("count", 20)
            }
            Log.d(TAG, "refreshAPODs...inserting... -> ${apods.size}")
            apodDao.insertApods(apods.filter { it.media_type == "image" })
            apodDao.setLastUpdate(LastUpdate("apod", System.currentTimeMillis()))
        } catch (e: Exception) {
            Log.d(TAG, "refreshAPODs...failed: $e")
        }
    }

    suspend fun fetchApodsForMonth(monthYear: String) {
        // TODO
        // fetch apods for an entire month
    }

    suspend fun getLastUpdate(): Long {
        return apodDao.getLastUpdate("apod")?.timetamp ?: -1
    }
}
