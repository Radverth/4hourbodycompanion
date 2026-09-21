package com.tom.fourhourbody.data.repo

import com.tom.fourhourbody.data.dao.NutritionDao
import com.tom.fourhourbody.data.entity.DamageControlLogEntity
import com.tom.fourhourbody.data.entity.DietDayLogEntity
import com.tom.fourhourbody.data.entity.DietMode
import com.tom.fourhourbody.data.entity.MealLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class NutritionRepository(private val dao: NutritionDao) {

    fun observeDay(date: LocalDate): Flow<DietDayLogEntity?> = dao.observeDay(date)

    fun observeMeals(dietDayId: Long): Flow<List<MealLogEntity>> = dao.observeMeals(dietDayId)

    fun observeDamageControl(dietDayId: Long): Flow<DamageControlLogEntity?> =
        dao.observeDamageControl(dietDayId)

    fun observeDaysBetween(from: LocalDate, to: LocalDate): Flow<List<DietDayLogEntity>> =
        dao.observeDaysBetween(from, to)

    /** The day row is created lazily, the first time anything about the day is touched. */
    suspend fun ensureDay(date: LocalDate, defaultMode: DietMode): DietDayLogEntity {
        dao.getDay(date)?.let { return it }
        val created = DietDayLogEntity(date = date, mode = defaultMode)
        val id = dao.upsertDay(created)
        return dao.getDay(date) ?: created.copy(id = id)
    }

    suspend fun updateDay(date: LocalDate, defaultMode: DietMode, transform: (DietDayLogEntity) -> DietDayLogEntity) {
        val day = ensureDay(date, defaultMode)
        dao.upsertDay(transform(day))
    }

    /**
     * Toggling the cheat day swaps which checklist is shown. Nothing else about the day is
     * touched — meals, notes and any damage-control taps all survive the toggle.
     */
    suspend fun setCheatDay(date: LocalDate, defaultMode: DietMode, isCheatDay: Boolean) {
        updateDay(date, defaultMode) { it.copy(isCheatDay = isCheatDay) }
        if (isCheatDay) {
            val day = ensureDay(date, defaultMode)
            if (dao.getDamageControl(day.id) == null) {
                dao.upsertDamageControl(DamageControlLogEntity(dietDayId = day.id))
            }
        }
    }

    suspend fun dayOn(date: LocalDate): DietDayLogEntity? = dao.getDay(date)

    suspend fun mealsOn(dietDayId: Long): List<MealLogEntity> = dao.getMeals(dietDayId)

    /**
     * Copies a previous day's meals and rule ticks forward. Slow-Carb Rule 2 is "eat the same
     * few meals over and over", so repeating a day is the normal case and should cost one tap;
     * typing it out again is what stops a food log getting filled in.
     *
     * The cheat-day flag is deliberately not copied — that is always a deliberate choice.
     */
    suspend fun copyDayForward(from: LocalDate, to: LocalDate, defaultMode: DietMode): Boolean {
        val previous = dao.getDay(from) ?: return false
        val target = ensureDay(to, defaultMode)

        dao.upsertDay(
            target.copy(
                mode = previous.mode,
                avoidedWhiteCarbs = previous.avoidedWhiteCarbs,
                noLiquidCalories = previous.noLiquidCalories,
                noFruit = previous.noFruit
            )
        )

        val existing = dao.getMeals(target.id).associateBy { it.mealSlot }
        dao.getMeals(previous.id).forEach { meal ->
            dao.upsertMeal(
                meal.copy(
                    id = existing[meal.mealSlot]?.id ?: 0,
                    dietDayId = target.id
                )
            )
        }
        return true
    }

    suspend fun upsertMeal(meal: MealLogEntity) = dao.upsertMeal(meal)

    suspend fun deleteMeal(meal: MealLogEntity) = dao.deleteMeal(meal)

    suspend fun updateDamageControl(
        dietDayId: Long,
        transform: (DamageControlLogEntity) -> DamageControlLogEntity
    ) {
        val existing = dao.getDamageControl(dietDayId) ?: DamageControlLogEntity(dietDayId = dietDayId)
        dao.upsertDamageControl(transform(existing))
    }
}
