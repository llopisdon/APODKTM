package com.machineinteractive.apodktm

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface ApodDao {

    // TODO - add query by date range
    @Query("SELECT * FROM apod ORDER BY date DESC")
    fun getAll(): Flow<List<Apod>>
    @Query("SELECT * FROM apod WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getApods(startDate: LocalDate, endDate: LocalDate): Flow<List<Apod>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApods(apods: List<Apod>)

    @Query("SELECT * FROM lastupdate WHERE id = :id")
    suspend fun getLastUpdate(id: String): LastUpdate?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLastUpdate(lastUpdate: LastUpdate)
}
