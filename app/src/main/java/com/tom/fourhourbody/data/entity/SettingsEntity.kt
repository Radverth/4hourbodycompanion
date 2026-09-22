package com.tom.fourhourbody.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/**
 * Single-row settings table (id is always 1). Times are stored as minutes since midnight so
 * they survive locale and timezone changes without a formatter round-trip.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,

    val reminderTimeMinutes: Int = 18 * 60,
    val weighInDay: DayOfWeek = DayOfWeek.SATURDAY,
    val weighInTimeMinutes: Int = 8 * 60,

    /**
     * An implementation intention — "I will train after X". Stating when and where roughly
     * doubles follow-through, so the app asks once and plays the answer back at the moment of
     * decision rather than issuing a generic reminder.
     */
    val trainingIntention: String? = null,

    /** One-time cue about breathing and the inroading mindset; dismissible, hence persisted. */
    val firstSetCueDismissed: Boolean = false
)
