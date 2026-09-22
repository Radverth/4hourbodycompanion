package com.tom.fourhourbody.domain.training

/**
 * The Big Five, as Body by Science actually specifies it. One set to positive failure per
 * exercise; time under load is the real measure of a set, not the rep count.
 */
object TrainingConstants {

    /**
     * The book's target ceiling: if a set is still going at 90 seconds, the weight was
     * misjudged. Crossing this is what earns the next session's weight bump.
     */
    const val TUL_CEILING_SEC = 90

    /** The book's own worked example is "5 to 10 percent"; this app applies the low end. */
    const val PROGRESSION_STEP_PERCENT = 0.05

    /**
     * A cadence guide only, shown on screen — not enforced. The book's rule of thumb is "as
     * slow as you can without the movement turning into a series of stops and starts," and
     * gives 10 seconds up / 10 seconds down as its own worked example.
     */
    const val TEMPO_UP_SEC = 10
    const val TEMPO_DOWN_SEC = 10

    /** "30 seconds to a minute... move briskly." This app times the midpoint. */
    const val REST_BETWEEN_EXERCISES_SEC = 45

    /**
     * Rest days between sessions at the start — a gap of 7 days, i.e. "once every seven days."
     * A plateau pushes this to 8, then 9, then further, the book's own mechanism for backing
     * off as a trainee gets stronger.
     */
    const val INITIAL_REST_DAYS = 6

    /**
     * Shown once before the first set of a session, dismissible. Covers the two things the
     * book asks for that a lifter would not otherwise know to do: breathe rather than hold the
     * breath, and keep pushing rather than bail the instant the weight stops moving.
     */
    const val FIRST_SET_CUE =
        "Breathe continuously through the set — open mouth, faster as it gets hard. Holding " +
            "your breath raises blood pressure and works against the point of the set.\n\n" +
            "When the weight stops moving, that is not the end: keep contracting against it. " +
            "The set is not about the weight going up and down, it's about reaching a real " +
            "level of fatigue — so panicking and bailing the moment it bogs down throws away " +
            "the part of the set that was actually doing something."
}
