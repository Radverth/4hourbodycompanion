package com.tom.fourhourbody.domain.training

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Frequency is rule-driven, not a fixed Mon/Thu. The gap starts at seven rest days — the
 * book's own default of roughly once a week — and grows by one every time a session stalls.
 *
 * A gap that keeps widening is the mechanism working, not the trainee slipping. As you get
 * stronger you take longer to recover from a set that goes all the way to failure, so the
 * schedule has to stretch to match; the book is explicit that going out to ten or fourteen
 * days costs nothing. The dashboard reads from [isUnusuallyLongGap] so it can say that
 * rather than counting days at you.
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

    /**
     * Past this, the gap is worth a second look — not because the protocol objects, but
     * because at some width the explanation stops being recovery and starts being something
     * else, and the app should not keep congratulating you either way.
     */
    fun isUnusuallyLongGap(restDaysBetween: Int): Boolean =
        restDaysBetween > TrainingConstants.LONG_GAP_REST_DAYS

    /** How many sessions the current gap would fit into a window of [windowDays] days. */
    fun expectedSessionsIn(windowDays: Int, restDaysBetween: Int): Int {
        val gap = restDaysBetween + 1
        if (gap <= 0) return 0
        return windowDays / gap
    }
}
