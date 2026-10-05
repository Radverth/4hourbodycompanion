package com.tom.fourhourbody.domain.training

import java.time.LocalDate

/**
 * How one completed session turned out, with the exercise it ended on.
 *
 * The session player stops at the stall, so the last row logged is the exercise that stalled.
 * That is what [PlateauRules] counts consecutive stalls on, and reading it as a projection
 * avoids pulling every log row back just to find the last one.
 *
 * It lives in the domain rather than beside the query that fills it because the rules are what
 * give it meaning, and a pure rule importing from the DAO layer has the dependency backwards.
 * Room is happy to populate any plain data class.
 */
data class SessionOutcome(
    val sessionId: Long,
    val date: LocalDate,
    val stalled: Boolean,
    val lastExercise: String?
)
