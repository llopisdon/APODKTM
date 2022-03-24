package com.machineinteractive.apodktm

import android.util.Log
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.*

@Dao
interface ApodDao {
    @Query("SELECT * FROM apod ORDER BY date DESC")
    fun getAll(): Flow<List<Apod>>
    @Query("SELECT * FROM apod WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getApods(startDate: LocalDate, endDate: LocalDate): Flow<List<Apod>>

    @Query("SELECT COUNT(date) FROM apod WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getApodsCount(startDate: LocalDate, endDate: LocalDate): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun _insertApods(apods: List<Apod>)

    @Query("SELECT * FROM lastupdate WHERE id = :id")
    suspend fun getLastUpdate(id: String): LastUpdate?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun _setLastUpdate(lastUpdate: LastUpdate)
    @Query("SELECT * FROM apod WHERE date = :date")
    suspend fun getApodForDay(date: LocalDate): Apod?

    @Transaction
    suspend fun insertApods(apods: List<Apod>, lastUpdateId: String) {
        val timestamp = if (apods.isEmpty()) {
            0L
        } else {
            Clock.System.now().toEpochMilliseconds()
        }

        Log.d(
            TAG,
            "\tHAS APODS: ${!apods.isEmpty()} num APODS: ${apods.count()} | set last update - id: $lastUpdateId timestamp: ${
                Instant.fromEpochMilliseconds(timestamp)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
            }"
        )
        _insertApods(apods)
        _setLastUpdate(LastUpdate(lastUpdateId, timestamp))
    }
}
