package com.tom.fourhourbody.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.tom.fourhourbody.data.entity.StretchConfigEntity
import com.tom.fourhourbody.data.entity.StretchLogEntity
import com.tom.fourhourbody.data.entity.StretchRoutine
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface StretchDao {

    @Insert
    suspend fun insertLog(log: StretchLogEntity): Long

    @Insert
    suspend fun insertLogs(logs: List<StretchLogEntity>)

    @Query("SELECT * FROM stretch_logs WHERE date = :date ORDER BY id DESC")
    fun observeLogsOn(date: LocalDate): Flow<List<StretchLogEntity>>

    @Query("SELECT * FROM stretch_logs WHERE date BETWEEN :from AND :to ORDER BY date DESC, id DESC")
    fun observeLogsBetween(from: LocalDate, to: LocalDate): Flow<List<StretchLogEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM stretch_logs
        WHERE routine = :routine AND sessionId IS NULL AND date BETWEEN :from AND :to
        """
    )
    fun countStandaloneBetween(routine: StretchRoutine, from: LocalDate, to: LocalDate): Flow<Int>

    @Query("SELECT * FROM stretch_configs ORDER BY routine ASC, orderIndex ASC, id ASC")
    fun observeAllConfigs(): Flow<List<StretchConfigEntity>>

    @Query(
        "SELECT * FROM stretch_configs WHERE routine = :routine AND isActive = 1 ORDER BY orderIndex ASC, id ASC"
    )
    suspend fun getConfigsFor(routine: StretchRoutine): List<StretchConfigEntity>

    @Upsert
    suspend fun upsertConfig(config: StretchConfigEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConfigs(configs: List<StretchConfigEntity>)

    @Query("SELECT COUNT(*) FROM stretch_configs")
    suspend fun countConfigs(): Int
}
