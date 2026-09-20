package com.tom.fourhourbody.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "sessions", indices = [Index("date")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val completed: Boolean = false,
    /** True when the target rep count was missed by more than one rep on any exercise. */
    val stalled: Boolean = false,
    val notes: String? = null
)

@Entity(tableName = "exercise_logs", indices = [Index("sessionId"), Index("exerciseName")])
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseName: String,
    val equipment: String,
    val weightKg: Double,
    val reps: Int,
    /** 7 for everything except leg press, which is 10. */
    val targetReps: Int,
    /** 5 seconds up, 5 seconds down. */
    val tempo: String = "5/5",
    /** Actual rest taken before this exercise, in seconds — the book asks for exactly 180. */
    val restSecActual: Int = 0
)

@Entity(tableName = "exercise_configs")
data class ExerciseConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slotName: String,
    val exerciseName: String,
    val equipment: String,
    val targetReps: Int,
    val isActive: Boolean = true,
    /** Order within a session. Not in the brief's table, but the slots are ordered. */
    val orderIndex: Int = 0
)

@Entity(tableName = "kettlebell_rounds", indices = [Index("sessionId")])
data class KettlebellRoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val roundNumber: Int,
    val swingCount: Int,
    val bellWeightKg: Double
)

/**
 * Single-row table (id is always 1). Rest days start at 2 and the book's stall rule pushes
 * them to 3, then 4+, which is what actually drives scheduling — not a fixed Mon/Thu.
 */
@Entity(tableName = "frequency_setting")
data class FrequencySettingEntity(
    @PrimaryKey val id: Int = 1,
    val currentRestDaysBetweenSessions: Int = 2,
    val lastIncreaseDate: LocalDate? = null
)
