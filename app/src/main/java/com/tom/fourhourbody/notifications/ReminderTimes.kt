package com.tom.fourhourbody.notifications

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure next-occurrence maths, kept out of the scheduler so it can be reasoned about (and
 * tested) without an AlarmManager.
 */
object ReminderTimes {

    fun nextDaily(now: LocalDateTime, minutesOfDay: Int): LocalDateTime {
        val today = now.toLocalDate().atTime(timeOf(minutesOfDay))
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    fun nextWeekly(now: LocalDateTime, day: DayOfWeek, minutesOfDay: Int): LocalDateTime {
        var candidate = now.toLocalDate().atTime(timeOf(minutesOfDay))
        repeat(8) {
            if (candidate.dayOfWeek == day && candidate.isAfter(now)) return candidate
            candidate = candidate.toLocalDate().plusDays(1).atTime(timeOf(minutesOfDay))
        }
        return candidate
    }

    /** A few hours after the session reminder, but never past bedtime. */
    fun nudgeMinutes(reminderTimeMinutes: Int): Int =
        minOf(reminderTimeMinutes + 3 * 60, 21 * 60 + 30)

    private fun timeOf(minutesOfDay: Int): LocalTime {
        val clamped = minutesOfDay.coerceIn(0, 24 * 60 - 1)
        return LocalTime.of(clamped / 60, clamped % 60)
    }
}
