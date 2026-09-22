package com.tom.fourhourbody.domain.today

/**
 * The one thing Today is about.
 *
 * With a single pillar there is only ever one thing that can be due, so this is a plain
 * on/off — but keeping it as a named rule rather than an inline `if` is what makes Today read
 * as "the one thing this is about" rather than "whatever happened to be checked first."
 */
enum class Focus {
    /** A session is due and has not been done. */
    TRAIN,

    /** Nothing is due. */
    CLEAR
}

data class FocusInputs(
    val sessionDueToday: Boolean = false,
    val sessionCompletedToday: Boolean = false
)

object FocusRules {

    fun pick(inputs: FocusInputs): Focus =
        if (inputs.sessionDueToday && !inputs.sessionCompletedToday) Focus.TRAIN else Focus.CLEAR
}
