package com.tom.fourhourbody.data.repo

import com.tom.fourhourbody.data.dao.TrainingDao
import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.FrequencySettingEntity
import com.tom.fourhourbody.data.entity.KettlebellRoundEntity
import com.tom.fourhourbody.data.entity.SessionEntity
import com.tom.fourhourbody.domain.training.ProgressionEngine
import com.tom.fourhourbody.domain.training.SessionScheduler
import com.tom.fourhourbody.domain.training.TrainingConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** What the dashboard and the session player need to know about scheduling. */
data class TrainingSchedule(
    val lastSessionDate: LocalDate?,
    val restDaysBetween: Int,
    val nextSessionDate: LocalDate?,
    val dueToday: Boolean,
    val daysUntilNext: Long
)

class TrainingRepository(private val dao: TrainingDao) {

    val allConfigs: Flow<List<ExerciseConfigEntity>> = dao.observeAllConfigs()

    val sessions: Flow<List<SessionEntity>> = dao.observeSessions()

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

    suspend fun activeStrengthConfigs(): List<ExerciseConfigEntity> =
        dao.getActiveConfigs().filterNot { it.equipment == TrainingConstants.KETTLEBELL_EQUIPMENT }

    suspend fun upsertConfig(config: ExerciseConfigEntity) = dao.upsertConfig(config)

    suspend fun frequency(): FrequencySettingEntity =
        dao.getFrequency() ?: FrequencySettingEntity().also { dao.upsertFrequency(it) }

    /** The opening weight to show for an exercise, already stepped up if the last set hit. */
    suspend fun openingWeightFor(config: ExerciseConfigEntity): Double? {
        val last = dao.getLastLogFor(config.exerciseName) ?: return null
        return ProgressionEngine.openingWeightFor(last.weightKg, last.reps, config.targetReps)
    }

    suspend fun lastLogFor(exerciseName: String): ExerciseLogEntity? = dao.getLastLogFor(exerciseName)

    /**
     * A session row is created when the session starts, not when it ends, so inline stretch
     * logs have a session to attach to and a session abandoned mid-way still leaves a record.
     */
    suspend fun startSession(date: LocalDate): Long =
        dao.insertSession(SessionEntity(date = date, completed = false))

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

}
