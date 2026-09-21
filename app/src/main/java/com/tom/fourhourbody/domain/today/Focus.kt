package com.tom.fourhourbody.domain.today

/**
 * The one thing Today is about.
 *
 * The screen used to show every pillar at once and let the reader work out which mattered.
 * Deciding here instead is the whole point: a screen that answers one question can be read
 * without being studied, and the rest of the day's state still fits underneath as marks.
 */
enum class Focus {
    /** A session is due and has not been done. Nothing outranks it. */
    TRAIN,

    /** Half of a pairing has landed today, so the other half is the smallest useful step. */
    COMBO,

    /** Nothing is due, but the day has not been logged — one tap closes it. */
    LOG_DAY,

    /** Everything that applied is done, or nothing applies yet. */
    CLEAR
}

data class FocusInputs(
    val trainingEnabled: Boolean = false,
    val sessionDueToday: Boolean = false,
    val sessionCompletedToday: Boolean = false,
    val comboHalfOpen: Boolean = false,
    val nutritionEnabled: Boolean = false,
    val dayLogged: Boolean = false
)

object FocusRules {

    /**
     * Ordered by what costs most to miss. Training is the pillar the whole protocol hangs on
     * and it only comes round every few days, so a due session outranks everything. A combo
     * beats a plain log because half of it is already spent.
     */
    fun pick(inputs: FocusInputs): Focus = when {
        inputs.trainingEnabled && inputs.sessionDueToday && !inputs.sessionCompletedToday ->
            Focus.TRAIN

        inputs.comboHalfOpen -> Focus.COMBO

        inputs.nutritionEnabled && !inputs.dayLogged -> Focus.LOG_DAY

        else -> Focus.CLEAR
    }
}
