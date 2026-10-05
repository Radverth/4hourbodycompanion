package com.tom.fourhourbody.domain.training

import com.tom.fourhourbody.data.entity.ExerciseLogEntity

/** An exercise that has stalled more than once in a row, and how many times. */
data class Plateau(val exerciseName: String, val consecutiveStalls: Int)

/**
 * When the sticking-point techniques are worth offering, and — more importantly — when they
 * are not.
 *
 * The book is unusually blunt that these cost more recovery than the plateau they are meant
 * to break, so reaching for them every session makes things worse rather than slower. That
 * makes the restraint part of the feature: this returns null far more often than it returns a
 * plateau, and the app never raises the subject on its own until the log says the same
 * exercise has genuinely stopped twice running.
 */
object PlateauRules {

    /**
     * The exercise currently plateaued, if any.
     *
     * Counts backward from the most recent session and stops at the first session that did
     * not stall. A single stall is how a run is supposed to end, so it earns a rest day and
     * nothing else; only a second one in a row on the same exercise means the added rest did
     * not resolve it, which is the case these techniques exist for.
     *
     * An unrelated exercise stalling in between breaks the chain, because then whatever is
     * going on is not specific to one movement and a sticking-point technique is the wrong
     * tool.
     */
    fun plateau(
        outcomes: List<SessionOutcome>,
        threshold: Int = TrainingConstants.STALLS_BEFORE_PLATEAU_PROMPT
    ): Plateau? {
        val newestFirst = outcomes.sortedWith(
            compareByDescending<SessionOutcome> { it.date }.thenByDescending { it.sessionId }
        )
        val first = newestFirst.firstOrNull() ?: return null
        if (!first.stalled) return null
        val exercise = first.lastExercise ?: return null

        var streak = 0
        for (outcome in newestFirst) {
            if (!outcome.stalled || outcome.lastExercise != exercise) break
            streak++
        }
        return if (streak >= threshold) Plateau(exercise, streak) else null
    }

    /**
     * Consecutive sessions at this exact load, newest first, that cleared the upper end of
     * the window — the evidence a board position step asks for.
     *
     * Stops at the first set that was at a different load or failed to clear, because the
     * question is whether *this* position is consistently finished with, and a clearance at
     * some other position says nothing about that.
     */
    fun clearancesAtLoad(
        recentNewestFirst: List<ExerciseLogEntity>,
        load: Load,
        targetMaxSec: Int = TrainingConstants.TARGET_TUL_MAX_SEC
    ): Int {
        var count = 0
        for (log in recentNewestFirst) {
            val logLoad = Load(log.weightKg, log.seatPosition)
            if (!logLoad.sameAs(load)) break
            if (log.tulSeconds <= targetMaxSec) break
            count++
        }
        return count
    }
}
