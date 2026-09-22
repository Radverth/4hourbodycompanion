package com.tom.fourhourbody.domain.adherence

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Training's completion over a window. [applicable] is the honest denominator — not the
 * number of days in the window, since training happens on a gap the frequency rule sets, not
 * daily.
 */
data class PillarAdherence(
    val completed: Int,
    val applicable: Int,
    val detail: String
) {
    val percent: Int
        get() = if (applicable <= 0) 0 else min(100, ((completed * 100.0) / applicable).roundToInt())
}

object AdherenceRules {

    fun training(completedSessions: Int, expectedSessions: Int): PillarAdherence =
        PillarAdherence(
            completed = completedSessions,
            applicable = expectedSessions,
            detail = "$completedSessions of $expectedSessions scheduled sessions"
        )
}
