package com.tom.fourhourbody.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** How a run ended. A run that is still going has neither. */
enum class RunEnd { PLATEAU, MANUAL }

/**
 * A run: the block of training between one plateau and the next.
 *
 * This is not a metaphor laid over the protocol — the book already works this way. You push
 * weight (or time under load) up session after session until one exercise fails to beat its
 * last result at the same weight, and that plateau ends the block and buys you another rest
 * day. The run is that block. What carries over is everything that matters: the weights, the
 * records, the wider gap that makes the next block work.
 */
@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runNumber: Int,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val endedBy: RunEnd? = null,
    val restDaysAtStart: Int,
    val restDaysAtEnd: Int? = null
) {
    val isActive: Boolean get() = endDate == null
}

@Entity(tableName = "sessions", indices = [Index("date"), Index("runId")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    /** The run this session belongs to; null only for sessions logged before runs existed. */
    val runId: Long? = null,
    val completed: Boolean = false,
    /** True when any exercise failed to beat its last time under load at the same weight. */
    @ColumnInfo(name = "stalled")
    val plateaued: Boolean = false,
    /** Which exercise triggered [plateaued] — null when the session didn't plateau. */
    val plateauedOnExercise: String? = null,
    val notes: String? = null
)

@Entity(tableName = "exercise_logs", indices = [Index("sessionId"), Index("exerciseName")])
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseName: String,
    val equipment: String,
    val weightKg: Double,
    /** Time under load, in seconds — the book's real measure of a set, not the rep count. */
    val tulSec: Int,
    /** Informational only; no rule reads this. Old sessions logged before TUL existed are 0. */
    val reps: Int = 0,
    /** Actual rest taken before this exercise, in seconds. */
    val restSecActual: Int = 0
)

@Entity(tableName = "exercise_configs")
data class ExerciseConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slotName: String,
    val exerciseName: String,
    val equipment: String,
    val isActive: Boolean = true,
    /** Order within a session. */
    val orderIndex: Int = 0
)

/**
 * Single-row table (id is always 1). Rest days start at 6 — "once every seven days" — and a
 * plateau pushes them to 7, then 8, then further, which is what actually drives scheduling.
 */
@Entity(tableName = "frequency_setting")
data class FrequencySettingEntity(
    @PrimaryKey val id: Int = 1,
    val currentRestDaysBetweenSessions: Int = 6,
    val lastIncreaseDate: LocalDate? = null
)
