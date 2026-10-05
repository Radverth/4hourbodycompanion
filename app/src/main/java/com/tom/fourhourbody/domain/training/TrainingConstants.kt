package com.tom.fourhourbody.domain.training

/**
 * Body by Science, as the book specifies it. This replaced Occam's Protocol wholesale rather
 * than being blended with it: Occam's progresses on reps at a 5/5 tempo, this progresses on
 * time under load at 10/10, and the two stall rules disagree about what a bad set even is.
 * Running half of each would produce a progression that is neither.
 */
object TrainingConstants {

    /**
     * The window a set is aiming for. Under 60s the load is too heavy to accumulate the
     * stimulus; over 90s it is light enough that next session should be heavier.
     */
    const val TARGET_TUL_MIN_SEC = 60
    const val TARGET_TUL_MAX_SEC = 90

    /**
     * 10 up, 10 down — but genuinely adjustable, and stored per log rather than assumed. The
     * book's actual instruction is a cadence slow enough to remove momentum and smooth enough
     * not to become a stop-start grind, which is a different number for different people on
     * different machines.
     */
    const val TEMPO_UP_SEC = 10
    const val TEMPO_DOWN_SEC = 10

    /**
     * 45 seconds, and deliberately not a gate. The book frames the gap between exercises as
     * "move briskly" — the point is to stay out of breath, not to wait out a timer, so the
     * session player lets you go early and logs what you actually took.
     */
    const val REST_BETWEEN_EXERCISES_SEC = 45

    /** The book's starting frequency: one session a week. The stall rule only widens it. */
    const val INITIAL_REST_DAYS = 7

    /**
     * Beyond this the gap stops being "recovering" and starts being "stopped". The book is
     * explicit that 10–14 days loses nothing, so the dashboard says so instead of nagging —
     * but there is a point past which something else is going on, and that is worth naming.
     */
    const val LONG_GAP_REST_DAYS = 14

    /** A successful set earns 5–10% more load. Both ends are offered; neither is imposed. */
    const val PROGRESSION_PERCENT_LOW = 0.05
    const val PROGRESSION_PERCENT_HIGH = 0.10

    /** Machines rarely adjust finer than this, so a suggestion finer than this is noise. */
    const val WEIGHT_INCREMENT_KG = 0.5

    /** Two stalls on the same exercise is when the plateau toolkit is worth offering. */
    const val STALLS_BEFORE_PLATEAU_PROMPT = 2

    /** Kept only to read back sessions logged under the old conditioning block. */
    const val KETTLEBELL_EQUIPMENT = "Kettlebell"

    const val EQUIPMENT_BOARD = "Board"
    const val EQUIPMENT_BODYWEIGHT = "Bodyweight"

    /**
     * Shown once before the first exercise, dismissible. Carried over from the previous
     * protocol because it is a shoulder-safety cue, not a progression rule — it applies to
     * any loaded pressing or pulling regardless of which book set the reps.
     */
    const val LOCKED_POSITION_CUE =
        "Lock your position: pull the shoulder blades back and down 1–2 inches, and hold " +
            "them there for the whole set. At a 10-second cadence there is no momentum to " +
            "hide behind, so the position has to be held rather than re-found each rep."

    /**
     * The one piece of record-keeping the book singles out as easy to skip and expensive to
     * lose: a seat an inch or two off changes the leverage, and with it the TUL you are
     * comparing against.
     */
    const val SEAT_POSITION_CUE =
        "Log the seat or pin setting. A 1–2 inch difference changes the leverage enough to " +
            "move your TUL on its own, which makes the comparison with last session useless."
}
