package com.machineinteractive.apodktm

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApodDao {

    // TODO - add query by date range
    @Query("SELECT * FROM apod ORDER BY date ASC")
    fun getAll(): Flow<List<Apod>>
    @Query("SELECT * FROM apod WHERE date = :date")
    fun getApod(date: String): Flow<Apod>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApods(apods: List<Apod>)

    @Query("SELECT * FROM lastupdate WHERE id = :id")
    suspend fun getLastUpdate(id: String): LastUpdate?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLastUpdate(lastUpdate: LastUpdate)
}
