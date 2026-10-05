package com.tom.fourhourbody.domain

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A sleep log row is keyed on the date the night *begins*. The checklist is mostly filled at
 * bedtime and the quality rating the morning after, so "the current night" flips over in the
 * late afternoon rather than at midnight.
 */
object SleepNight {

    const val NIGHT_ROLLOVER_HOUR = 17

    fun currentNightDate(now: LocalDateTime): LocalDate =
        if (now.hour >= NIGHT_ROLLOVER_HOUR) now.toLocalDate() else now.toLocalDate().minusDays(1)
}

/**
 * What the recovery chapter asks for, and the values the picker offers.
 *
 * Half-hour steps from five to nine and a half: fine enough to be honest about a short night,
 * coarse enough that logging it is one tap rather than a number pad at bedtime. Nothing here
 * is a target to fail — the hours are recorded so a run of five-hour nights is visible next
 * to a stalling lift, which is the connection the book draws.
 */
object SleepTargets {

    const val MIN_HOURS = 7.0
    const val MAX_HOURS = 9.0

    /** For copy, so "7–9" never renders as "7.0–9.0". */
    const val RANGE_LABEL = "7–9"

    val CHOICES: List<Double> = (10..19).map { it / 2.0 }

    fun label(hours: Double): String =
        if (hours % 1.0 == 0.0) "${hours.toInt()}h" else "${hours.toInt()}h30"

    fun withinTarget(hours: Double?): Boolean = hours != null && hours >= MIN_HOURS
}
