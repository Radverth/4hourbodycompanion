package com.tom.fourhourbody.domain.training

import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.SessionEntity

/** An exercise that has plateaued more than once in a row, and how many times. */
data class StickingPoint(val exerciseName: String, val consecutivePlateaus: Int)

/**
 * When the book's sticking-point techniques are worth offering, and — more importantly — when
 * they are not.
 *
 * The book is unusually blunt that these cost more recovery than the plateau they are meant to
 * break, so reaching for them every session makes things worse rather than slower. That makes
 * the restraint part of the feature: this returns null far more often than it returns a
 * sticking point, and the app never raises the subject until the log says the same exercise
 * has genuinely stopped twice running.
 *
 * Named for the technique rather than for the plateau because "plateau" already means one bad
 * session everywhere else in this app, and a second meaning for it would make both unclear.
 */
object StickingPointRules {

    /** Two in a row is the threshold. One plateau is how a run is supposed to end. */
    const val PLATEAUS_BEFORE_OFFER = 2

    /**
     * The exercise currently stuck, if any.
     *
     * Counts backward from the most recent completed session and stops at the first one that
     * did not plateau. A single plateau already earns a rest day and nothing else; only a
     * second in a row on the same exercise means the added rest did not resolve it, which is
     * the case these techniques exist for.
     *
     * An unrelated exercise plateauing in between breaks the chain, because then whatever is
     * going on is not specific to one movement and a sticking-point technique is the wrong
     * tool for it.
     */
    fun stickingPoint(
        sessions: List<SessionEntity>,
        threshold: Int = PLATEAUS_BEFORE_OFFER
    ): StickingPoint? {
        val newestFirst = sessions
            .filter { it.completed }
            .sortedWith(compareByDescending<SessionEntity> { it.date }.thenByDescending { it.id })

        val first = newestFirst.firstOrNull() ?: return null
        if (!first.plateaued) return null
        val exercise = first.plateauedOnExercise ?: return null

        var streak = 0
        for (session in newestFirst) {
            if (!session.plateaued || session.plateauedOnExercise != exercise) break
            streak++
        }
        return if (streak >= threshold) StickingPoint(exercise, streak) else null
    }

    /**
     * Consecutive recent sets at this exact board position, newest first, that cleared the
     * ceiling — the evidence a position advance asks for.
     *
     * Stops at the first set that was at a different position or failed to clear, because the
     * question is whether *this* position is consistently finished with, and a clearance
     * somewhere else on the ladder says nothing about that.
     */
    fun clearancesAtPosition(
        recentNewestFirst: List<ExerciseLogEntity>,
        position: String?,
        ceilingSec: Int = TrainingConstants.TUL_CEILING_SEC
    ): Int {
        val wanted = position?.trim()?.takeIf { it.isNotEmpty() }
        var count = 0
        for (log in recentNewestFirst) {
            if (log.position?.trim()?.takeIf { it.isNotEmpty() } != wanted) break
            if (log.tulSec < ceilingSec) break
            count++
        }
        return count
    }
}
