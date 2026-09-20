package com.tom.fourhourbody.notifications

import com.tom.fourhourbody.data.entity.SettingsEntity
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure next-occurrence maths, kept out of the scheduler so it can be reasoned about (and
 * tested) without an AlarmManager.
 */
object ReminderTimes {

    fun nextDaily(now: LocalDateTime, minutesOfDay: Int): LocalDateTime {
        val candidate = now.toLocalDate().atTime(timeOf(minutesOfDay))
        return if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
    }

    fun nextWeekly(now: LocalDateTime, day: DayOfWeek, minutesOfDay: Int): LocalDateTime {
        var candidate = now.toLocalDate().atTime(timeOf(minutesOfDay))
        while (candidate.dayOfWeek != day || !candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    /** The next tick of the every-N-hours desk-reset window, on a configured work day. */
    fun nextDeskResetTicks(now: LocalDateTime, settings: SettingsEntity): List<LocalDateTime> {
        if (settings.deskResetIntervalHours <= 0) return emptyList()
        if (settings.deskResetEndMinutes <= settings.deskResetStartMinutes) return emptyList()

        val ticks = mutableListOf<LocalDateTime>()
        var date = now.toLocalDate()
        var daysChecked = 0
        while (ticks.isEmpty() && daysChecked < 8) {
            if (settings.deskResetDays.contains(date.dayOfWeek)) {
                var minutes = settings.deskResetStartMinutes + settings.deskResetIntervalHours * 60
                while (minutes <= settings.deskResetEndMinutes &&
                    ticks.size < ReminderKind.MAX_DESK_RESET_TICKS
                ) {
                    val candidate = date.atTime(timeOf(minutes))
                    if (candidate.isAfter(now)) ticks += candidate
                    minutes += settings.deskResetIntervalHours * 60
                }
            }
            date = date.plusDays(1)
            daysChecked++
        }
        return ticks
    }

    fun nextColdReminder(now: LocalDateTime, settings: SettingsEntity): LocalDateTime? {
        if (settings.coldReminderDays.isEmpty()) return null
        return settings.coldReminderDays
            .map { nextWeekly(now, it, settings.coldReminderMinutes) }
            .minOrNull()
    }

    /** A few hours after the session reminder, but never past bedtime. */
    fun nudgeMinutes(settings: SettingsEntity): Int =
        minOf(settings.reminderTimeMinutes + 3 * 60, 21 * 60 + 30)

    private fun timeOf(minutesOfDay: Int): LocalTime {
        val clamped = minutesOfDay.coerceIn(0, 24 * 60 - 1)
        return LocalTime.of(clamped / 60, clamped % 60)
    }
}
