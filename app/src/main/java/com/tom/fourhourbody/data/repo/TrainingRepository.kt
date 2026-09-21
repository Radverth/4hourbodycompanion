package com.tom.fourhourbody.data.repo

import com.tom.fourhourbody.data.dao.TrainingDao
import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.FrequencySettingEntity
import com.tom.fourhourbody.data.entity.KettlebellRoundEntity
import com.tom.fourhourbody.data.entity.RunEnd
import com.tom.fourhourbody.data.entity.RunEntity
import com.tom.fourhourbody.data.entity.SessionEntity
import com.tom.fourhourbody.domain.run.RunEngine
import com.tom.fourhourbody.domain.run.RunSummary
import com.tom.fourhourbody.domain.training.ProgressionEngine
import com.tom.fourhourbody.domain.training.SessionScheduler
import com.tom.fourhourbody.domain.training.TrainingConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** What the dashboard and the session player need to know about scheduling. */
data class TrainingSchedule(
    val lastSessionDate: LocalDate?,
    val restDaysBetween: Int,
    val nextSessionDate: LocalDate?,
    val dueToday: Boolean,
    val daysUntilNext: Long
)

/**
 * Where training stands, phrased so it always reads — before the first session as much as
 * mid-block. An empty app still has a run number, and saying "run 1, not started" is worth
 * more than saying nothing at all.
 */
data class RunStatus(
    val runNumber: Int,
    val sessionsThisRun: Int,
    val started: Boolean
)

class TrainingRepository(private val dao: TrainingDao) {

    val allConfigs: Flow<List<ExerciseConfigEntity>> = dao.observeAllConfigs()

    val sessions: Flow<List<SessionEntity>> = dao.observeSessions()

    /** Every logged set from a completed session, oldest first. */
    val completedLogs: Flow<List<ExerciseLogEntity>> = dao.observeCompletedLogs()

    fun schedule(today: LocalDate): Flow<TrainingSchedule> =
        combine(dao.observeLastCompletedSession(), dao.observeFrequency()) { last, freq ->
            val restDays = freq?.currentRestDaysBetweenSessions ?: TrainingConstants.INITIAL_REST_DAYS
            val lastDate = last?.date
            TrainingSchedule(
                lastSessionDate = lastDate,
                restDaysBetween = restDays,
                nextSessionDate = lastDate?.let { SessionScheduler.nextSessionDate(it, restDays) },
                dueToday = SessionScheduler.isSessionDue(today, lastDate, restDays),
                daysUntilNext = SessionScheduler.daysUntilNextSession(today, lastDate, restDays)
            )
        }

    fun countCompletedBetween(from: LocalDate, to: LocalDate): Flow<Int> =
        dao.countCompletedBetween(from, to)

    fun observeSessionsOn(date: LocalDate): Flow<List<SessionEntity>> = dao.observeSessionsOn(date)

    fun sessionsBetween(from: LocalDate, to: LocalDate): Flow<List<SessionEntity>> =
        dao.observeSessionsBetween(from, to)

    suspend fun activeStrengthConfigs(): List<ExerciseConfigEntity> =
        dao.getActiveConfigs().filterNot { it.equipment == TrainingConstants.KETTLEBELL_EQUIPMENT }

    suspend fun config(id: Long): ExerciseConfigEntity? = dao.getConfig(id)

    suspend fun upsertConfig(config: ExerciseConfigEntity) = dao.upsertConfig(config)

    suspend fun frequency(): FrequencySettingEntity =
        dao.getFrequency() ?: FrequencySettingEntity().also { dao.upsertFrequency(it) }

    /** The opening weight to show for an exercise, already stepped up if the last set hit. */
    suspend fun openingWeightFor(config: ExerciseConfigEntity): Double? {
        val last = dao.getLastLogFor(config.exerciseName) ?: return null
        return ProgressionEngine.openingWeightFor(last.weightKg, last.reps, config.targetReps)
    }

    suspend fun lastLogFor(exerciseName: String): ExerciseLogEntity? = dao.getLastLogFor(exerciseName)

    suspend fun bestEverFor(exerciseName: String): Double? = dao.getBestEverFor(exerciseName)

    /**
     * A session row is created when the session starts, not when it ends, so inline stretch
     * logs have a session to attach to and a session abandoned mid-way still leaves a record.
     * Starting a session with no run open opens one — the first session of a run is what
     * begins it, so the user never has to declare a block before training.
     */
    suspend fun startSession(date: LocalDate): Long {
        val run = currentRun(date)
        return dao.insertSession(SessionEntity(date = date, runId = run.id, completed = false))
    }

    suspend fun logExercise(log: ExerciseLogEntity): Long = dao.insertExerciseLog(log)

    suspend fun logKettlebellRound(round: KettlebellRoundEntity): Long =
        dao.insertKettlebellRound(round)

    suspend fun finishSession(sessionId: Long, stalled: Boolean, notes: String?) {
        val session = dao.getSession(sessionId) ?: return
        dao.updateSession(session.copy(completed = true, stalled = stalled, notes = notes))
    }

    /**
     * The book's frequency mechanism: a stalled session adds a rest day to every session that
     * follows. Called once, when the stalled session is saved.
     */
    suspend fun applyStall(today: LocalDate): Int {
        val current = frequency()
        val updated = current.copy(
            currentRestDaysBetweenSessions =
                SessionScheduler.restDaysAfterStall(current.currentRestDaysBetweenSessions),
            lastIncreaseDate = today
        )
        dao.upsertFrequency(updated)
        return updated.currentRestDaysBetweenSessions
    }

    // ---- Runs -------------------------------------------------------------------------
    //
    // A run is the block of training between one stall and the next. The protocol already
    // works in blocks; naming them just makes the shape visible, and gives the weights
    // somewhere to be banked when a block closes.

    val activeRun: Flow<RunEntity?> = dao.observeActiveRun()

    /** The open run, opening one if none is. */
    suspend fun currentRun(today: LocalDate): RunEntity {
        dao.getActiveRun()?.let { return it }
        val run = RunEntity(
            runNumber = RunEngine.nextRunNumber(dao.getRuns()),
            startDate = today,
            restDaysAtStart = frequency().currentRestDaysBetweenSessions
        )
        val id = dao.insertRun(run)
        return run.copy(id = id)
    }

    suspend fun completedSessionsInCurrentRun(): Int =
        dao.getActiveRun()?.let { dao.countCompletedInRun(it.id) } ?: 0

    /**
     * Closes the open run and reads back what it earned. Called after [applyStall] so the
     * rest days recorded on the run are the ones the next run will actually use.
     */
    suspend fun endRun(endedBy: RunEnd, today: LocalDate): RunSummary? {
        val run = dao.getActiveRun() ?: return null
        val closed = run.copy(
            endDate = today,
            endedBy = endedBy,
            restDaysAtEnd = frequency().currentRestDaysBetweenSessions
        )
        dao.updateRun(closed)
        return summarise(closed, today)
    }

    suspend fun summarise(run: RunEntity, today: LocalDate): RunSummary =
        RunEngine.summarise(
            run = run,
            sessions = dao.getSessionsForRun(run.id),
            logs = dao.getLogsForRun(run.id),
            today = today
        )

    /** Never empty: with no run open it reports the number the next one will take. */
    fun runStatus(): Flow<RunStatus> =
        combine(dao.observeActiveRun(), dao.observeRuns(), dao.observeSessions()) { active, runs, sessions ->
            if (active == null) {
                RunStatus(RunEngine.nextRunNumber(runs), sessionsThisRun = 0, started = false)
            } else {
                RunStatus(
                    runNumber = active.runNumber,
                    sessionsThisRun = sessions.count { it.runId == active.id && it.completed },
                    started = true
                )
            }
        }

    /** Every run, newest first — the open one included, so a run in progress still reads. */
    fun runHistory(today: LocalDate): Flow<List<RunSummary>> =
        dao.observeRuns().map { runs -> runs.map { summarise(it, today) } }
}
