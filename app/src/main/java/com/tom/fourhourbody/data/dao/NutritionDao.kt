package com.tom.fourhourbody.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.tom.fourhourbody.data.entity.DamageControlLogEntity
import com.tom.fourhourbody.data.entity.DietDayLogEntity
import com.tom.fourhourbody.data.entity.MealLogEntity
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
    suspend fun upsertMeal(meal: MealLogEntity): Long

    @Delete
    suspend fun deleteMeal(meal: MealLogEntity)

    @Query("SELECT * FROM meal_logs WHERE dietDayId = :dietDayId ORDER BY mealSlot ASC, id ASC")
    fun observeMeals(dietDayId: Long): Flow<List<MealLogEntity>>

    @Query("SELECT * FROM meal_logs WHERE dietDayId = :dietDayId ORDER BY mealSlot ASC, id ASC")
    suspend fun getMeals(dietDayId: Long): List<MealLogEntity>

    @Upsert
    suspend fun upsertDamageControl(log: DamageControlLogEntity): Long

    @Query("SELECT * FROM damage_control_logs WHERE dietDayId = :dietDayId")
    fun observeDamageControl(dietDayId: Long): Flow<DamageControlLogEntity?>

    @Query("SELECT * FROM damage_control_logs WHERE dietDayId = :dietDayId")
    suspend fun getDamageControl(dietDayId: Long): DamageControlLogEntity?
}
