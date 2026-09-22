package com.tom.fourhourbody.domain.run

import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.RunEnd
import com.tom.fourhourbody.data.entity.RunEntity
import com.tom.fourhourbody.data.entity.SessionEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** What one exercise gained across a run. */
data class ExerciseGain(
    val exerciseName: String,
    val fromKg: Double,
    val toKg: Double
) {
    val gainedKg: Double get() = toKg - fromKg
    val improved: Boolean get() = gainedKg > 0.01
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
    val plateauedOn: String?,
    val restDaysBefore: Int,
    val restDaysAfter: Int,
    val endedBy: RunEnd?
) {
    /** Total weight added across every exercise — the run's headline number. */
    val totalGainKg: Double get() = gains.sumOf { it.gainedKg }.coerceAtLeast(0.0)

    val isComplete: Boolean get() = endedBy != null
}

/**
 * Reads runs out of the training log.
 *
 * A plateau is not a failure state and the summary should never read like one. Failing to beat
 * a previous time under load at the same weight is the protocol's own signal that the gap
 * between sessions is now too short — the block did its job, the weights it earned are kept,
 * and the next block runs on more rest. Ending a run is the mechanism working, not the user
 * falling short.
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
                ExerciseGain(name, first.weightKg, last.weightKg)
            }
            .sortedByDescending { it.gainedKg }

        val plateauedSession = completed.lastOrNull { it.plateaued }
        val plateauedOn = plateauedSession?.plateauedOnExercise

        return RunSummary(
            runNumber = run.runNumber,
            sessions = completed.size,
            days = ChronoUnit.DAYS.between(run.startDate, run.endDate ?: today) + 1,
            gains = gains,
            plateauedOn = plateauedOn,
            restDaysBefore = run.restDaysAtStart,
            restDaysAfter = run.restDaysAtEnd ?: run.restDaysAtStart,
            endedBy = run.endedBy
        )
    }

    /** The next run's number, so numbering survives gaps and deletions. */
    fun nextRunNumber(runs: List<RunEntity>): Int = (runs.maxOfOrNull { it.runNumber } ?: 0) + 1
}
