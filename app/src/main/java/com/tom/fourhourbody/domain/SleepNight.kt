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
