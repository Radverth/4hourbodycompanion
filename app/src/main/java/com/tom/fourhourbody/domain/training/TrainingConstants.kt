package com.tom.fourhourbody.domain.training

/**
 * Occam's Protocol, as the book actually specifies it. Earlier scoping used approximate
 * figures (8–12 reps, a fixed twice-weekly split); these are the real numbers and the rest of
 * the training pillar reads them from here rather than hardcoding its own.
 */
object TrainingConstants {

    /** Failure target for every exercise except leg press. */
    const val DEFAULT_TARGET_REPS = 7

    /** Leg press goes to 10+. */
    const val LEG_PRESS_TARGET_REPS = 10

    const val TEMPO_UP_SEC = 5
    const val TEMPO_DOWN_SEC = 5

    /** Exactly three minutes between exercises — timed, not eyeballed. */
    const val REST_BETWEEN_EXERCISES_SEC = 180

    /**
     * A miss of one rep is not a stall. More than one rep short of target is, and it ends the
     * session on the spot.
     */
    const val STALL_TOLERANCE_REPS = 1

    /** Rest days between sessions at the start; the stall rule pushes this to 3, then 4+. */
    const val INITIAL_REST_DAYS = 2

    const val KETTLEBELL_EQUIPMENT = "Kettlebell"

    const val TABATA_ROUNDS = 8
    const val TABATA_WORK_SEC = 20
    const val TABATA_REST_SEC = 10

    const val SIX_MINUTE_ABS_SEC = 360

    /** 10 lb in kilograms — the floor of the progression step. */
    const val TEN_POUNDS_KG = 4.5359237

    const val PROGRESSION_PERCENT = 0.10

    /**
     * Shown once before the first exercise of a session, dismissible. The book applies this
     * cue to every loaded exercise, not just one lift.
     */
    const val LOCKED_POSITION_CUE =
        "Lock your position: pull the shoulder blades back and down 1–2 inches, and hold " +
            "them there for every rep of every loaded exercise. This is the shoulder-safety " +
            "cue the book applies across the whole session, not just to pressing."
}
