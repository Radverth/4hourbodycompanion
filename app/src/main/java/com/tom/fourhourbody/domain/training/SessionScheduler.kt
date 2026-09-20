package com.tom.fourhourbody.domain.training

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Frequency is rule-driven, not a fixed Mon/Thu. The gap starts at two rest days and grows by
 * one every time a session stalls, which is the book's actual mechanism for backing off.
 */
object SessionScheduler {

    /** Rest days are the days *between* sessions, so the gap is restDays + 1. */
    fun nextSessionDate(lastSessionDate: LocalDate, restDaysBetween: Int): LocalDate =
        lastSessionDate.plusDays((restDaysBetween + 1).toLong())

    fun isSessionDue(today: LocalDate, lastSessionDate: LocalDate?, restDaysBetween: Int): Boolean {
        if (lastSessionDate == null) return true
        return !today.isBefore(nextSessionDate(lastSessionDate, restDaysBetween))
    }

    fun daysUntilNextSession(
        today: LocalDate,
        lastSessionDate: LocalDate?,
        restDaysBetween: Int
    ): Long {
        if (lastSessionDate == null) return 0
        val next = nextSessionDate(lastSessionDate, restDaysBetween)
        return maxOf(0L, ChronoUnit.DAYS.between(today, next))
    }

    /** Applied when a session stalls: one more rest day for everything that follows. */
    fun restDaysAfterStall(currentRestDays: Int): Int = currentRestDays + 1

    /** How many sessions the current gap would fit into a window of [windowDays] days. */
    fun expectedSessionsIn(windowDays: Int, restDaysBetween: Int): Int {
        val gap = restDaysBetween + 1
        if (gap <= 0) return 0
        return windowDays / gap
    }
}
