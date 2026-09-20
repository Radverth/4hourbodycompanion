package com.tom.fourhourbody.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.tom.fourhourbody.data.entity.ColdExposureLogEntity
import com.tom.fourhourbody.data.entity.CreatineLogEntity
import com.tom.fourhourbody.data.entity.MeasurementEntity
import com.tom.fourhourbody.data.entity.SleepLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SleepDao {

    /** Upsert on the unique date index — re-opening a day's checklist edits, never duplicates. */
    @Upsert
    suspend fun upsert(log: SleepLogEntity): Long

    @Query("SELECT * FROM sleep_logs WHERE date = :date")
    suspend fun get(date: LocalDate): SleepLogEntity?

    @Query("SELECT * FROM sleep_logs WHERE date = :date")
    fun observe(date: LocalDate): Flow<SleepLogEntity?>

    @Query("SELECT * FROM sleep_logs WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<SleepLogEntity>>
}

@Dao
interface ColdDao {

    @Insert
    suspend fun insert(log: ColdExposureLogEntity): Long

    @Delete
    suspend fun delete(log: ColdExposureLogEntity)

    @Query("SELECT * FROM cold_exposure_logs WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<ColdExposureLogEntity>>

    @Query("SELECT COUNT(*) FROM cold_exposure_logs WHERE date BETWEEN :from AND :to")
    fun countBetween(from: LocalDate, to: LocalDate): Flow<Int>

    @Query("SELECT * FROM cold_exposure_logs ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ColdExposureLogEntity>>
}

@Dao
interface CreatineDao {

    @Upsert
    suspend fun upsert(log: CreatineLogEntity): Long

    @Query("SELECT * FROM creatine_logs WHERE date = :date")
    suspend fun get(date: LocalDate): CreatineLogEntity?

    @Query("SELECT * FROM creatine_logs WHERE date = :date")
    fun observe(date: LocalDate): Flow<CreatineLogEntity?>

    @Query("SELECT * FROM creatine_logs WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<CreatineLogEntity>>

    /** Rows outside the new cycle are stale once a cycle restarts. */
    @Query("DELETE FROM creatine_logs WHERE date < :from")
    suspend fun deleteBefore(from: LocalDate)
}

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
