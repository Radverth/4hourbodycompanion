package com.tom.fourhourbody.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * The brief describes a `ruleFlags` group; it is modelled as discrete boolean columns so the
 * adherence queries can count them directly instead of unpacking a blob.
 */
@Entity(tableName = "diet_day_logs", indices = [Index(value = ["date"], unique = true)])
data class DietDayLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val mode: DietMode,
    val avoidedWhiteCarbs: Boolean = false,
    val noLiquidCalories: Boolean = false,
    val noFruit: Boolean = false,
    val isCheatDay: Boolean = false,
    val notes: String? = null
)

/** Surfaced only when the day is flagged as a cheat day. A checklist, never a requirement. */
@Entity(tableName = "damage_control_logs", indices = [Index(value = ["dietDayId"], unique = true)])
data class DamageControlLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dietDayId: Long,
    val proteinFiberFirstMeal: Boolean = false,
    val citrusBeforeBigMeal: Boolean = false,
    val movementBeforeMeal: Boolean = false,
    val movementAfterMeal: Boolean = false,
    val walkedAfterMeal: Boolean = false
)

/**
 * The book's "Synergize" fat-loss checklist, and nothing more than a checklist.
 *
 * Deliberately not called Synergy — that name already belongs to the combos, which pair two
 * things you logged. This is the other sense of the word: four levers that the fat-loss
 * chapter says compound with the training rather than substituting for it. Eat food that
 * hasn't been processed; stay cool, because a body kept slightly cold spends energy
 * reheating itself; drink enough; keep stress down. The fifth lever in the book is training
 * hard, which is the rest of this app, so it is not restated here as something to tick.
 *
 * There are no targets to fail. [hydrationLiters] carries a number because litres are worth
 * counting; everything else is a tap, and a day with none of them ticked is a logged day
 * rather than a bad score.
 */
@Entity(tableName = "synergize_logs", indices = [Index(value = ["date"], unique = true)])
data class SynergizeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val ateUnprocessed: Boolean = false,
    val keptCoolToday: Boolean = false,
    /** Litres, roughly 3 a day. Cold water logged in the cold pillar counts toward it. */
    val hydrationLiters: Double? = null,
    val stressManaged: Boolean = false
)
