package com.tom.fourhourbody.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** How a run ended. A run that is still going has neither. */
enum class RunEnd { STALL, MANUAL }

/** Which exercise set a session runs. All three are overrides, not edits to the slot list. */
enum class SessionKind(val label: String) {
    /** Whatever the slot config says — Big Five by default, Big Three if that toggle is on. */
    STANDARD("Standard"),

    /** Cutting phase: leg press plus one alternating upper-body exercise. */
    CUTTING("Cutting"),

    /** Board and bodyweight rows only, for a night without machines. */
    NO_EQUIPMENT("No equipment")
}

/** The sticking-point techniques, used sparingly and only after repeated stalls. */
enum class PlateauTechnique(val label: String, val detail: String) {
    REST_PAUSE(
        "Rest-pause",
        "At failure, pause 5–30 seconds — the shorter end for a lift that already took 90s " +
            "or more to get there — then squeeze out one more rep."
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
 * A run: the block of training between one stall and the next.
 *
 * This is not a metaphor laid over the protocol — the book already works this way. You push
 * load up session after session until a set fails to match the time it managed last time, and
 * that miss ends the block and buys you another rest day. The run is that block, and the
 * stall is how it ends. What carries over is everything that matters: the loads, the records,
 * the wider gap that makes the next block work.
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
    /** True when a set failed to match the time it managed last session at the same load. */
    val stalled: Boolean = false,
    /**
     * Wall-clock length of the whole session. Worth keeping alongside summed TUL: the book
     * points out that if the total creeps up while the time under load doesn't, the rests
     * between exercises have quietly got longer without anyone deciding they should.
     */
    @ColumnInfo(defaultValue = "0")
    val elapsedSessionTimeSec: Int = 0,
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
    /**
     * Seat, pin or handle position. Nullable because not every exercise has one, but for
     * board work it carries the whole progression — the handle position *is* the load.
     */
    val seatPosition: String? = null,
    /** Seconds to positive failure. This is the number progression is decided on. */
    val tulSeconds: Int,
    /**
     * Rep count, kept because it is free to count and occasionally interesting. It is not an
     * input to any rule: at a fixed cadence it is TUL divided by the cadence, and the moment
     * the cadence drifts it stops meaning anything.
     */
    val reps: Int? = null,
    val targetTulMinSec: Int = 60,
    val targetTulMaxSec: Int = 90,
    /** The cadence actually used, e.g. "10/10". Stored because the book says to adjust it. */
    val repCadenceSec: String = "10/10",
    /** Rest actually taken before this exercise, in seconds. 45 is the target, not a gate. */
    val restSecActual: Int = 0,
    /**
     * True for rows whose TUL was computed from a rep count logged under the old rep-based
     * protocol rather than timed. Marked rather than hidden: the figure is a reasonable
     * reading of what happened, but it was not measured, and a character sheet that cannot
     * tell the difference is one you stop trusting.
     */
    val tulDerived: Boolean = false
)

@Entity(tableName = "exercise_configs")
data class ExerciseConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slotName: String,
    val exerciseName: String,
    val equipment: String,
    /** What to use for this slot without a machine. No honest free-weight pulldown exists. */
    val freeWeightEquivalent: String? = null,
    val targetTulMinSec: Int = 60,
    val targetTulMaxSec: Int = 90,
    val isActive: Boolean = true,
    /** Order within a session. The book fixes the order, so it is data rather than a sort. */
    val orderIndex: Int = 0
) {
    val isBodyweight: Boolean
        get() = equipment.equals("Board", ignoreCase = true) ||
            equipment.equals("Bodyweight", ignoreCase = true)
}

/** Kept to read back the conditioning block logged under the previous protocol. */
@Entity(tableName = "kettlebell_rounds", indices = [Index("sessionId")])
data class KettlebellRoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val roundNumber: Int,
    val swingCount: Int,
    val bellWeightKg: Double
)

/** A log, nothing more. Which technique was reached for, so overuse is at least visible. */
@Entity(tableName = "plateau_technique_logs", indices = [Index("sessionId"), Index("date")])
data class PlateauTechniqueLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long?,
    val date: LocalDate,
    val exerciseName: String,
    val technique: PlateauTechnique,
    val notes: String? = null
)

/**
 * Single-row table (id is always 1). Rest days start at the book's once-a-week default and
 * the stall rule widens them from there, which is what actually drives scheduling.
 */
@Entity(tableName = "frequency_setting")
data class FrequencySettingEntity(
    @PrimaryKey val id: Int = 1,
    val currentRestDaysBetweenSessions: Int = 7,
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
