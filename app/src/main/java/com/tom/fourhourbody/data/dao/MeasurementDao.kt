package com.tom.fourhourbody.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.tom.fourhourbody.data.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MeasurementDao {

    @Upsert
    suspend fun upsert(measurement: MeasurementEntity): Long

    @Delete
    suspend fun delete(measurement: MeasurementEntity)

    @Query("SELECT * FROM measurements ORDER BY date ASC, id ASC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements ORDER BY date DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<MeasurementEntity?>

    @Query("SELECT * FROM measurements WHERE date = :date ORDER BY id DESC LIMIT 1")
    suspend fun getOn(date: LocalDate): MeasurementEntity?
}
