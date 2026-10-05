package com.tom.fourhourbody.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tom.fourhourbody.data.entity.PlateauTechniqueLogEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface PlateauDao {

    @Insert
    suspend fun insert(log: PlateauTechniqueLogEntity): Long

    @Query("SELECT * FROM plateau_technique_logs ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<PlateauTechniqueLogEntity>>

    @Query("SELECT * FROM plateau_technique_logs WHERE exerciseName = :exerciseName ORDER BY date DESC, id DESC")
    fun observeFor(exerciseName: String): Flow<List<PlateauTechniqueLogEntity>>

    @Query("SELECT COUNT(*) FROM plateau_technique_logs WHERE date BETWEEN :from AND :to")
    suspend fun countBetween(from: LocalDate, to: LocalDate): Int
}
