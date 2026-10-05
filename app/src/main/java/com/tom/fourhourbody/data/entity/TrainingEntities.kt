package com.tom.fourhourbody.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tom.fourhourbody.domain.training.TrainingConstants
import java.time.LocalDate

/** How a run ended. A run that is still going has neither. */
enum class RunEnd { PLATEAU, MANUAL }

/**
 * Which exercise set a session runs. All three are overrides applied when the session is
 * generated, never edits to the stored slot list.
 */
enum class SessionKind(val label: String) {
    /** Whatever the slot list says — the Big Five, or the Big Three if that toggle is on. */
    STANDARD("Standard"),

    /** Cutting phase: leg press plus one alternating upper-body exercise. */
    CUTTING("Cutting"),

    /** Board and bodyweight rows only, for a night without machines. */
    NO_EQUIPMENT("No equipment")
}

/**
 * The book's sticking-point techniques, for an exercise that has plateaued twice running.
 *
 * Offered rather than suggested, and logged rather than acted on: the book is explicit that
 * overusing these wrecks recovery worse than the plateau they are meant to fix, so the only
 * thing the app does with one is record that it was used.
 */
enum class StickingPointTechnique(val label: String, val detail: String) {
    REST_PAUSE(
        "Rest-pause",
        "At failure, pause 5–30 seconds — the shorter end for a lift that already took 90 " +
            "seconds or more to get there — then squeeze out one more rep."
    ),
    TIMED_STATIC_HOLD(
        "Timed static hold",
        "At the sticking point, hold the weight statically for around 10 seconds instead of " +
            "racking it."
    ),
    NEGATIVE_ONLY(
        "Negative-only",
        "Skip the lifting phase entirely and work only the lowering. Failure here means you " +
            "can no longer take 5 or more seconds to lower it."
    ),
    PARTIAL_REPS(
        "Partial reps",
        "Work only the strongest part of the range, which lets you push more load through it."
    )
}

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
    @ColumnInfo(defaultValue = "'STANDARD'")
    val kind: SessionKind = SessionKind.STANDARD,
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
    /**
     * Seat, pin or handle setting. The book calls this out as easy to overlook and expensive
     * to lose: an inch or two changes the leverage, and with it the time you are comparing
     * against. On the board it carries the whole progression, because the handle position is
     * the only thing that can change.
     */
    val position: String? = null,
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
) {
    /**
     * Board and bodyweight rows are picked by equipment when a no-equipment session starts,
     * so they never appear in a normal session and never need toggling off.
     */
    val isBodyweight: Boolean
        get() = equipment.equals(TrainingConstants.EQUIPMENT_BOARD, ignoreCase = true) ||
            equipment.equals(TrainingConstants.EQUIPMENT_BODYWEIGHT, ignoreCase = true)
}

/** A log and nothing more: which technique was reached for, so overuse is at least visible. */
@Entity(tableName = "sticking_point_logs", indices = [Index("sessionId"), Index("date")])
data class StickingPointLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long? = null,
    val date: LocalDate,
    val exerciseName: String,
    val technique: StickingPointTechnique,
    val notes: String? = null
)

/**
 * Single-row table (id is always 1). Rest days start at 6 — "once every seven days" — and a
 * plateau pushes them to 7, then 8, then further, which is what actually drives scheduling.
 */
@Entity(tableName = "frequency_setting")
data class FrequencySettingEntity(
    @PrimaryKey val id: Int = 1,
    val currentRestDaysBetweenSessions: Int = 6,
    val lastIncreaseDate: LocalDate? = null,
    /**
     * A sustained calorie-deficit phase, not a one-session choice — which is why it lives
     * here rather than being a slot edit. The book's own fat-loss study found that cutting
     * training volume *down* during a deficit retained twice the muscle and lost twice the
     * fat, because dieting is already spending the recovery the training needs.
     */
    @ColumnInfo(defaultValue = "0")
    val cuttingPhaseActive: Boolean = false
)
