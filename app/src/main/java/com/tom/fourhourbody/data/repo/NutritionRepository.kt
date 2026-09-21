package com.tom.fourhourbody.data.repo

import com.tom.fourhourbody.data.dao.NutritionDao
import com.tom.fourhourbody.data.entity.DamageControlLogEntity
import com.tom.fourhourbody.data.entity.DietDayLogEntity
import com.tom.fourhourbody.data.entity.DietMode
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class NutritionRepository(private val dao: NutritionDao) {

    fun observeDay(date: LocalDate): Flow<DietDayLogEntity?> = dao.observeDay(date)

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
     * touched — notes and any damage-control taps survive the toggle either way.
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

    /**
     * The whole day in one tap. A compliant day is the common case, so it costs a single
     * action; the day something slipped is the one that costs more — which is the right way
     * round, and the opposite of asking for three ticks every evening.
     */
    suspend fun markDayClean(date: LocalDate, defaultMode: DietMode) {
        updateDay(date, defaultMode) {
            it.copy(
                avoidedWhiteCarbs = true,
                noLiquidCalories = true,
                noFruit = true
            )
        }
    }

    suspend fun updateDamageControl(
        dietDayId: Long,
        transform: (DamageControlLogEntity) -> DamageControlLogEntity
    ) {
        val existing = dao.getDamageControl(dietDayId) ?: DamageControlLogEntity(dietDayId = dietDayId)
        dao.upsertDamageControl(transform(existing))
    }
}
