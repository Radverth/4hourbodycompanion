package com.tom.fourhourbody.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tom.fourhourbody.data.entity.DamageControlLogEntity
import com.tom.fourhourbody.data.entity.DietDayLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Damage-control measures with the date of the day they belong to. The rows themselves are
 * keyed by diet day rather than by date, and the synergy rules work in days.
 */
data class DamageControlOnDate(
    val date: LocalDate,
    val proteinFiberFirstMeal: Boolean,
    val citrusBeforeBigMeal: Boolean,
    val movementBeforeMeal: Boolean,
    val movementAfterMeal: Boolean,
    val walkedAfterMeal: Boolean
) {
    val ticks: Int
        get() = listOf(
            proteinFiberFirstMeal,
            citrusBeforeBigMeal,
            movementBeforeMeal,
            movementAfterMeal,
            walkedAfterMeal
        ).count { it }
}

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

    /**
     * Damage-control rows over a range, carrying the date of the day they belong to — the
     * rows themselves are keyed by diet day, not by date.
     */
    @Query(
        """
        SELECT
            d.date AS date,
            dc.proteinFiberFirstMeal AS proteinFiberFirstMeal,
            dc.citrusBeforeBigMeal AS citrusBeforeBigMeal,
            dc.movementBeforeMeal AS movementBeforeMeal,
            dc.movementAfterMeal AS movementAfterMeal,
            dc.walkedAfterMeal AS walkedAfterMeal
        FROM damage_control_logs dc
        INNER JOIN diet_day_logs d ON d.id = dc.dietDayId
        WHERE d.date BETWEEN :from AND :to
        """
    )
    fun observeDamageControlBetween(from: LocalDate, to: LocalDate): Flow<List<DamageControlOnDate>>
}
