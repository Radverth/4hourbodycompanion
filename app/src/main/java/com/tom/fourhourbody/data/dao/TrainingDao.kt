package com.tom.fourhourbody.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.FrequencySettingEntity
import com.tom.fourhourbody.data.entity.KettlebellRoundEntity
import com.tom.fourhourbody.data.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TrainingDao {

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY date DESC, id DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE completed = 1 ORDER BY date DESC, id DESC LIMIT 1")
    fun observeLastCompletedSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE completed = 1 ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getLastCompletedSession(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE date = :date ORDER BY id DESC")
    fun observeSessionsOn(date: LocalDate): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions WHERE completed = 1 AND date BETWEEN :from AND :to")
    fun countCompletedBetween(from: LocalDate, to: LocalDate): Flow<Int>

    @Insert
    suspend fun insertExerciseLog(log: ExerciseLogEntity): Long

    /** Most recent logged set for an exercise — the input to the next weight suggestion. */
    @Query(
        """
        SELECT el.* FROM exercise_logs el
        INNER JOIN sessions s ON s.id = el.sessionId
        WHERE el.exerciseName = :exerciseName AND s.completed = 1
        ORDER BY s.date DESC, el.id DESC LIMIT 1
        """
    )
    suspend fun getLastLogFor(exerciseName: String): ExerciseLogEntity?

    @Insert
    suspend fun insertKettlebellRound(round: KettlebellRoundEntity): Long

    @Query("SELECT * FROM exercise_configs WHERE isActive = 1 ORDER BY orderIndex ASC, id ASC")
    fun observeActiveConfigs(): Flow<List<ExerciseConfigEntity>>

    @Query("SELECT * FROM exercise_configs ORDER BY orderIndex ASC, id ASC")
    fun observeAllConfigs(): Flow<List<ExerciseConfigEntity>>

    @Query("SELECT * FROM exercise_configs WHERE isActive = 1 ORDER BY orderIndex ASC, id ASC")
    suspend fun getActiveConfigs(): List<ExerciseConfigEntity>

    @Upsert
    suspend fun upsertConfig(config: ExerciseConfigEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConfigs(configs: List<ExerciseConfigEntity>)

    @Query("SELECT COUNT(*) FROM exercise_configs")
    suspend fun countConfigs(): Int

    @Upsert
    suspend fun upsertFrequency(setting: FrequencySettingEntity)

    @Query("SELECT * FROM frequency_setting WHERE id = 1")
    suspend fun getFrequency(): FrequencySettingEntity?

    @Query("SELECT * FROM frequency_setting WHERE id = 1")
    fun observeFrequency(): Flow<FrequencySettingEntity?>
}
