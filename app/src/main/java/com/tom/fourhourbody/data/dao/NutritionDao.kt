package com.tom.fourhourbody.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tom.fourhourbody.data.entity.DamageControlLogEntity
import com.tom.fourhourbody.data.entity.DietDayLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface NutritionDao {

    @Upsert
    suspend fun upsertDay(day: DietDayLogEntity): Long

    @Query("SELECT * FROM diet_day_logs WHERE date = :date")
    suspend fun getDay(date: LocalDate): DietDayLogEntity?

    @Query("SELECT * FROM diet_day_logs WHERE date = :date")
    fun observeDay(date: LocalDate): Flow<DietDayLogEntity?>

    @Query("SELECT * FROM diet_day_logs WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeDaysBetween(from: LocalDate, to: LocalDate): Flow<List<DietDayLogEntity>>

    @Upsert
    suspend fun upsertDamageControl(log: DamageControlLogEntity): Long

    @Query("SELECT * FROM damage_control_logs WHERE dietDayId = :dietDayId")
    fun observeDamageControl(dietDayId: Long): Flow<DamageControlLogEntity?>

    @Query("SELECT * FROM damage_control_logs WHERE dietDayId = :dietDayId")
    suspend fun getDamageControl(dietDayId: Long): DamageControlLogEntity?
}
