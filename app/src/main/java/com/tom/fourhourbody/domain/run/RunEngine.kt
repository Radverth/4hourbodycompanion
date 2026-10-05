package com.tom.fourhourbody.domain.run

import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.RunEnd
import com.tom.fourhourbody.data.entity.RunEntity
import com.tom.fourhourbody.data.entity.SessionEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * What one exercise gained across a run, on both axes the protocol moves.
 *
 * Load is the headline for anything on a machine. Time under load matters on its own for
 * board and bodyweight work, where the load cannot change by five percent — there the run's
 * progress is seconds, and a summary that only counted kilos would report nothing happened.
 */
data class ExerciseGain(
    val exerciseName: String,
    val fromKg: Double,
    val toKg: Double,
    val fromTulSec: Int = 0,
    val toTulSec: Int = 0
) {
    val gainedKg: Double get() = toKg - fromKg
    val gainedTulSec: Int get() = toTulSec - fromTulSec
    val improved: Boolean get() = gainedKg > 0.01 || (gainedKg <= 0.01 && gainedTulSec > 0)
}

/**
 * Everything worth saying about a finished run. Every figure is read back out of what was
 * logged — nothing here is awarded, and nothing is invented.
 */
data class RunSummary(
    val runNumber: Int,
    val sessions: Int,
    val days: Long,
    val gains: List<ExerciseGain>,
    val stalledOn: String?,
    val restDaysBefore: Int,
    val restDaysAfter: Int,
    val endedBy: RunEnd?
) {
    /** Total weight added across every exercise — the run's headline number. */
    val totalGainKg: Double get() = gains.sumOf { it.gainedKg }.coerceAtLeast(0.0)

    /** Seconds added, which is the only axis a bodyweight run can move. */
    val totalGainTulSec: Int get() = gains.sumOf { it.gainedTulSec }.coerceAtLeast(0)

    val isComplete: Boolean get() = endedBy != null
}

/**
 * Reads runs out of the training log.
 *
 * A stall is not a failure state and the summary should never read like one. Failing to match
 * the time a load held last session is the protocol's own signal that the gap between
 * sessions is now too short — the block did its job, the loads it earned are kept, and the
 * next block runs on more rest. Ending a run is the mechanism working, not the user falling
 * short.
 */
object RunEngine {

    fun summarise(
        run: RunEntity,
        sessions: List<SessionEntity>,
        logs: List<ExerciseLogEntity>,
        today: LocalDate = LocalDate.now()
    ): RunSummary {
        val completed = sessions.filter { it.completed }.sortedBy { it.date }
        val orderOf = sessions.associate { it.id to it.date }

        val gains = logs
            .groupBy { it.exerciseName }
            .mapNotNull { (name, entries) ->
                val ordered = entries.sortedWith(
                    compareBy({ orderOf[it.sessionId] ?: LocalDate.MIN }, { it.id })
                )
                val first = ordered.firstOrNull() ?: return@mapNotNull null
                val last = ordered.last()
                ExerciseGain(
                    exerciseName = name,
                    fromKg = first.weightKg,
                    toKg = last.weightKg,
                    fromTulSec = first.tulSeconds,
                    toTulSec = last.tulSeconds
                )
            }
            .sortedByDescending { it.gainedKg }

        // The session player stops at the stall, so the stalling exercise is whatever was
        // logged last in that session. Re-deriving it by comparing TULs would mean refetching
        // the previous session's logs to know what load each set was being judged against,
        // and would still only arrive back at this same row.
        val stalledSession = completed.lastOrNull { it.stalled }
        val stalledOn = stalledSession?.let { session ->
            logs.filter { it.sessionId == session.id }.maxByOrNull { it.id }?.exerciseName
        }

        return RunSummary(
            runNumber = run.runNumber,
            sessions = completed.size,
            days = ChronoUnit.DAYS.between(run.startDate, run.endDate ?: today) + 1,
            gains = gains,
            stalledOn = stalledOn,
            restDaysBefore = run.restDaysAtStart,
            restDaysAfter = run.restDaysAtEnd ?: run.restDaysAtStart,
            endedBy = run.endedBy
        )
    }

    /** The next run's number, so numbering survives gaps and deletions. */
    fun nextRunNumber(runs: List<RunEntity>): Int = (runs.maxOfOrNull { it.runNumber } ?: 0) + 1
}
