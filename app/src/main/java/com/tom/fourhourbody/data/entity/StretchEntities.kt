package com.tom.fourhourbody.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * One row per stretch performed. [sessionId] is set when the stretch was run inline from a
 * training session and null when it was done standalone.
 *
 * [date] and [routine] are not in the brief's field list but are needed by the dashboard and
 * adherence queries (a standalone log has no session to borrow a date from).
 */
@Entity(tableName = "stretch_logs", indices = [Index("sessionId"), Index("date")])
data class StretchLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long? = null,
    val stretchName: String,
    val sideOrPosition: String,
    val durationSec: Int,
    /** Set for rep-based entries such as Active Bridges; null for holds. */
    val repsCompleted: Int? = null,
    val routine: StretchRoutine,
    val date: LocalDate
)

/**
 * Editable seed list. Changing a hold time or rep target here changes it at every entry
 * point that uses the stretch — inline and standalone alike.
 */
@Entity(tableName = "stretch_configs")
data class StretchConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stretchName: String,
    val routine: StretchRoutine,
    val mode: StretchMode,
    val defaultHoldSec: Int? = null,
    val defaultReps: Int? = null,
    val defaultSets: Int? = null,
    /** Comma-separated sides or positions, e.g. "Non-dominant,Dominant". */
    val defaultSide: String? = null,
    val isActive: Boolean = true,
    val orderIndex: Int = 0,
    /**
     * Desk-reset only: true for the longer entries that belong to the weekly routine
     * (Active Bridges, Supine Groin Progressive, Air Bench) rather than the every-2-3-hours
     * interval set. Phase 3 needs two different reminders over one routine, hence the flag.
     */
    val isWeeklyOnly: Boolean = false,
    val notes: String? = null
)
