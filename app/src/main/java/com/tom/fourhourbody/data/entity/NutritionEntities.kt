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
