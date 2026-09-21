package com.tom.fourhourbody.notifications

import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.domain.shift.ShiftPattern
import com.tom.fourhourbody.domain.shift.ShiftSchedule
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure next-occurrence maths, kept out of the scheduler so it can be reasoned about (and
 * tested) without an AlarmManager.
 *
 * Every time here is passed through the shift rota before it is returned. A reminder that
 * fires while you are stuck at a desk cannot be acted on, and an unactionable prompt is not
 * neutral — it is how an app teaches you to swipe its notifications away.
 */
object ReminderTimes {

    /** A daily reminder, moved clear of the working day. */
    fun nextDaily(now: LocalDateTime, minutesOfDay: Int, pattern: ShiftPattern): LocalDateTime {
        var candidate = now.toLocalDate().atTime(timeOf(minutesOfDay))
        repeat(9) {
            val free = ShiftSchedule.moveOutOfShift(candidate, pattern)
            if (free.isAfter(now)) return free
            candidate = candidate.toLocalDate().plusDays(1).atTime(timeOf(minutesOfDay))
        }
        return candidate
    }

    fun nextWeekly(
        now: LocalDateTime,
        day: DayOfWeek,
        minutesOfDay: Int,
        pattern: ShiftPattern
    ): LocalDateTime {
        var candidate = now.toLocalDate().atTime(timeOf(minutesOfDay))
        repeat(22) {
            if (candidate.dayOfWeek == day) {
                val free = ShiftSchedule.moveOutOfShift(candidate, pattern)
                if (free.isAfter(now)) return free
            }
            candidate = candidate.toLocalDate().plusDays(1).atTime(timeOf(minutesOfDay))
        }
        return candidate
    }

    /**
     * When to prompt the desk reset.
     *
     * The book's version fires every two or three hours at a desk, which assumes you can lie
     * on the floor in an office. When you can't, the prompt moves to just after the shift —
     * the sitting still happened, so the reset still matters; only the moment you can act on
     * it has changed.
     */
    fun nextDeskResetTicks(
        now: LocalDateTime,
        settings: SettingsEntity
    ): List<LocalDateTime> {
        val pattern = settings.shiftPattern

        if (!pattern.enabled) {
            return legacyWindowTicks(now, settings)
        }

        if (!settings.canStretchAtWork) {
            var date = now.toLocalDate()
            repeat(9) {
                val at = ShiftSchedule.afterShiftOn(date, pattern)
                if (at != null && at.isAfter(now)) return listOf(at)
                date = date.plusDays(1)
            }
            return emptyList()
        }

        // Able to stretch at work: space the prompts through the shift itself.
        if (settings.deskResetIntervalHours <= 0) return emptyList()
        var date = now.toLocalDate()
        repeat(9) {
            val window = ShiftSchedule.windowOn(date, pattern)
            if (window != null) {
                val ticks = mutableListOf<LocalDateTime>()
                var minutes = window.first + settings.deskResetIntervalHours * 60
                while (minutes <= window.last && ticks.size < ReminderKind.MAX_DESK_RESET_TICKS) {
                    val at = date.atTime(timeOf(minutes))
                    if (at.isAfter(now)) ticks += at
                    minutes += settings.deskResetIntervalHours * 60
                }
                if (ticks.isNotEmpty()) return ticks
            }
            date = date.plusDays(1)
        }
        return emptyList()
    }

    /** How long you were sitting, so the prompt can say something true. */
    fun sittingHoursFor(at: LocalDateTime, settings: SettingsEntity): Int? =
        ShiftSchedule.shiftLengthHours(at.toLocalDate(), settings.shiftPattern)

    fun nextColdReminder(now: LocalDateTime, settings: SettingsEntity): LocalDateTime? {
        if (settings.coldReminderDays.isEmpty()) return null
        return settings.coldReminderDays
            .map { nextWeekly(now, it, settings.coldReminderMinutes, settings.shiftPattern) }
            .minOrNull()
    }

    /** A few hours after the session reminder, but never past bedtime. */
    fun nudgeMinutes(settings: SettingsEntity): Int =
        minOf(settings.reminderTimeMinutes + 3 * 60, 21 * 60 + 30)

    /** The fixed-window behaviour, for anyone not on a rota. */
    private fun legacyWindowTicks(
        now: LocalDateTime,
        settings: SettingsEntity
    ): List<LocalDateTime> {
        if (settings.deskResetIntervalHours <= 0) return emptyList()
        if (settings.deskResetEndMinutes <= settings.deskResetStartMinutes) return emptyList()

        val ticks = mutableListOf<LocalDateTime>()
        var date = now.toLocalDate()
        var daysChecked = 0
        while (ticks.isEmpty() && daysChecked < 8) {
            if (settings.workDays.contains(date.dayOfWeek)) {
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

    private fun timeOf(minutesOfDay: Int): LocalTime {
        val clamped = minutesOfDay.coerceIn(0, 24 * 60 - 1)
        return LocalTime.of(clamped / 60, clamped % 60)
    }
}
