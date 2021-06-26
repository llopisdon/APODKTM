package com.machineinteractive.apodktm

import io.ktor.client.*
import kotlinx.coroutines.flow.Flow

class ApodRepository(private val apodDao: ApodDao, private val client: HttpClient) {

    fun getApods(): Flow<List<Apod>> {
        return apodDao.getAll()
    }

    fun getApod(date: String): Flow<Apod> {
        return apodDao.getApod(date)
    }

    suspend fun refreshApods() {
        // TODO
        // fetch apods from today's date to start of month
    }

    suspend fun fetchApodsForMonth(monthYear: String) {
        // TODO
        // fetch apods for an entire month
    }
}
