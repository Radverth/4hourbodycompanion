package com.tom.fourhourbody.domain.training

import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.SessionKind

/** The five slots, in the order the book runs them. */
object Slots {
    const val PULL_ROW = "pull-row"
    const val PUSH = "push"
    const val PULL_LAT = "pull-lat"
    const val OVERHEAD = "overhead"
    const val LEGS = "legs"

    /** Row, chest press, pulldown, overhead press, leg press. */
    val BIG_FIVE = listOf(PULL_ROW, PUSH, PULL_LAT, OVERHEAD, LEGS)

    /** Drop the row and the overhead press; the remaining three cover the same movements. */
    val BIG_THREE = setOf(LEGS, PULL_LAT, PUSH)

    /** The two the cutting phase alternates between, one per session. */
    val CUTTING_UPPER = listOf(PUSH, PULL_ROW)
}

/**
 * Which exercises a session actually runs.
 *
 * Every variation here is an override applied when the session is generated, never an edit to
 * the slot list. That matters more than it sounds: switching to a cutting phase or a night
 * without machines is a temporary decision, and if it rewrote the configuration then turning
 * it back off would have to reconstruct what was there before — so one mistaken tap would
 * cost the setup. Leaving the stored list alone makes every one of these reversible by
 * definition.
 */
object SessionPlanner {

    /**
     * @param lastCuttingUpperSlot the upper-body slot used in the previous cutting session,
     *   read from the log rather than stored, so the alternation cannot drift out of step
     *   with what was actually trained.
     */
    fun exercisesFor(
        kind: SessionKind,
        configs: List<ExerciseConfigEntity>,
        bigThreeOnly: Boolean = false,
        lastCuttingUpperSlot: String? = null
    ): List<ExerciseConfigEntity> {
        val active = configs.filter { it.isActive }.sortedBy { it.orderIndex }
        return when (kind) {
            SessionKind.NO_EQUIPMENT -> active.filter { it.isBodyweight }

            SessionKind.CUTTING -> {
                val loaded = active.filterNot { it.isBodyweight }
                val upper = nextCuttingUpper(lastCuttingUpperSlot)
                loaded.filter { it.slotName == upper || it.slotName == Slots.LEGS }
            }

            SessionKind.STANDARD -> {
                val loaded = active.filterNot { it.isBodyweight }
                if (bigThreeOnly) loaded.filter { it.slotName in Slots.BIG_THREE } else loaded
            }
        }
    }

    /**
     * Chest press one session, seated row the next. With a gap of a week or more between
     * sessions this is the book's "one week, then the other" in practice, and alternating on
     * the session rather than the calendar keeps it true when the gap widens.
     */
    fun nextCuttingUpper(lastUpperSlot: String?): String {
        val index = Slots.CUTTING_UPPER.indexOf(lastUpperSlot)
        if (index < 0) return Slots.CUTTING_UPPER.first()
        return Slots.CUTTING_UPPER[(index + 1) % Slots.CUTTING_UPPER.size]
    }

    /** Which session kind to run, given the phase toggle and whether equipment is to hand. */
    fun kindFor(cuttingPhaseActive: Boolean, hasEquipment: Boolean): SessionKind = when {
        !hasEquipment -> SessionKind.NO_EQUIPMENT
        cuttingPhaseActive -> SessionKind.CUTTING
        else -> SessionKind.STANDARD
    }
}
