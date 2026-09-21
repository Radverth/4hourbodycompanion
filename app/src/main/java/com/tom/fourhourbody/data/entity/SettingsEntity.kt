package com.tom.fourhourbody.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Single-row settings table (id is always 1). Times are stored as minutes since midnight so
 * they survive locale and timezone changes without a formatter round-trip.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,

    /** Preferred training days. Advisory only — the stall rule drives the real gap. */
    val trainingDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
    val reminderTimeMinutes: Int = 18 * 60,
    val weighInDay: DayOfWeek = DayOfWeek.SATURDAY,
    val weighInTimeMinutes: Int = 8 * 60,
    val dietMode: DietMode = DietMode.SLOW_CARB,
    val creatineCycleStartDate: LocalDate? = null,

    // Pillar toggles — each independently switchable; a disabled pillar leaves the dashboard
    // and cancels its reminders without deleting any history.
    val trainingEnabled: Boolean = true,
    val stretchesEnabled: Boolean = true,
    val nutritionEnabled: Boolean = true,
    val sleepEnabled: Boolean = true,
    val coldEnabled: Boolean = true,
    val creatineEnabled: Boolean = true,

    // Reminder configuration, per pillar.
    val sleepReminderMinutes: Int = 21 * 60 + 30,
    val deskResetRemindersEnabled: Boolean = true,
    val deskResetStartMinutes: Int = 9 * 60,
    val deskResetEndMinutes: Int = 17 * 60,
    val deskResetIntervalHours: Int = 3,
    val deskResetDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ),
    val weeklyDeskResetDay: DayOfWeek = DayOfWeek.SUNDAY,
    val weeklyMobilityDay: DayOfWeek = DayOfWeek.WEDNESDAY,
    /** Time of day for both weekly routine reminders (full desk reset, rest-day mobility). */
    val weeklyRoutineTimeMinutes: Int = 10 * 60,
    val coldRemindersEnabled: Boolean = false,
    val coldReminderDays: Set<DayOfWeek> = setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
    val coldReminderMinutes: Int = 19 * 60,
    val creatineRemindersEnabled: Boolean = true,
    val creatineMorningMinutes: Int = 7 * 60,
    val creatineEveningMinutes: Int = 22 * 60,

    /**
     * Implementation intentions — "I will do X after Y". Stating when and where roughly
     * doubles follow-through, so the app asks once and then plays the answer back at the
     * moment of decision rather than issuing a generic reminder.
     */
    val trainingIntention: String? = null,
    val stretchesIntention: String? = null,
    val nutritionIntention: String? = null,
    val sleepIntention: String? = null,
    val coldIntention: String? = null,
    val creatineIntention: String? = null,

    /**
     * The cheat day is a scheduled release valve, so it is counted down to rather than
     * stumbled into — anticipating it is what makes the other six days sustainable.
     */
    @ColumnInfo(defaultValue = "6")
    val cheatDay: DayOfWeek = DayOfWeek.SATURDAY,

    /** One-time "locked position" form cue; dismissible, hence persisted. */
    val lockedPositionCueDismissed: Boolean = false,

    val defaultBellWeightKg: Double = 24.0,
    val sixMinuteAbsEnabled: Boolean = true
) {
    fun isEnabled(pillar: Pillar): Boolean = when (pillar) {
        Pillar.TRAINING -> trainingEnabled
        Pillar.STRETCHES -> stretchesEnabled
        Pillar.NUTRITION -> nutritionEnabled
        Pillar.SLEEP -> sleepEnabled
        Pillar.COLD -> coldEnabled
        Pillar.CREATINE -> creatineEnabled
    }

    fun withPillar(pillar: Pillar, enabled: Boolean): SettingsEntity = when (pillar) {
        Pillar.TRAINING -> copy(trainingEnabled = enabled)
        Pillar.STRETCHES -> copy(stretchesEnabled = enabled)
        Pillar.NUTRITION -> copy(nutritionEnabled = enabled)
        Pillar.SLEEP -> copy(sleepEnabled = enabled)
        Pillar.COLD -> copy(coldEnabled = enabled)
        Pillar.CREATINE -> copy(creatineEnabled = enabled)
    }

    val enabledPillars: List<Pillar> get() = Pillar.entries.filter { isEnabled(it) }

    fun intentionFor(pillar: Pillar): String? = when (pillar) {
        Pillar.TRAINING -> trainingIntention
        Pillar.STRETCHES -> stretchesIntention
        Pillar.NUTRITION -> nutritionIntention
        Pillar.SLEEP -> sleepIntention
        Pillar.COLD -> coldIntention
        Pillar.CREATINE -> creatineIntention
    }?.takeIf { it.isNotBlank() }

    fun withIntention(pillar: Pillar, intention: String?): SettingsEntity = when (pillar) {
        Pillar.TRAINING -> copy(trainingIntention = intention)
        Pillar.STRETCHES -> copy(stretchesIntention = intention)
        Pillar.NUTRITION -> copy(nutritionIntention = intention)
        Pillar.SLEEP -> copy(sleepIntention = intention)
        Pillar.COLD -> copy(coldIntention = intention)
        Pillar.CREATINE -> copy(creatineIntention = intention)
    }
}
