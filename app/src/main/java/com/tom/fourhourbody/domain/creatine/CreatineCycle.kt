package com.tom.fourhourbody.domain.creatine

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 3.5 g on waking, 3.5 g before bed, for 28 days. A log, not an advisor. */
object CreatineCycle {

    const val CYCLE_LENGTH_DAYS = 28
    const val DOSE_GRAMS = 3.5

    data class State(
        val started: Boolean,
        /** 1–28 while the cycle is running; null before it starts or after it finishes. */
        val day: Int?,
        val complete: Boolean,
        val startDate: LocalDate?
    ) {
        val daysRemaining: Int? get() = day?.let { CYCLE_LENGTH_DAYS - it }
    }

    fun stateOn(startDate: LocalDate?, date: LocalDate): State {
        if (startDate == null || date.isBefore(startDate)) {
            return State(started = false, day = null, complete = false, startDate = startDate)
        }
        val elapsed = ChronoUnit.DAYS.between(startDate, date).toInt() + 1
        return if (elapsed > CYCLE_LENGTH_DAYS) {
            State(started = true, day = null, complete = true, startDate = startDate)
        } else {
            State(started = true, day = elapsed, complete = false, startDate = startDate)
        }
    }

    /** Last date covered by the cycle that began on [startDate]. */
    fun endDate(startDate: LocalDate): LocalDate =
        startDate.plusDays((CYCLE_LENGTH_DAYS - 1).toLong())
}
