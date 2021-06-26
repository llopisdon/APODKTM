package com.machineinteractive.apodktm

import io.ktor.client.*

class ApodRepository(private val apodDao: ApodDao, private val client: HttpClient) {

    suspend fun refreshApods() {
        // TODO
        // fetch apods from today's date to start of month
    }

    suspend fun fetchApodsForMonth(monthYear: String) {
        // TODO
        // fetch apods for an entire month
    }
}
