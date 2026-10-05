package com.tom.fourhourbody.domain.training

import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.SessionKind

/** The seeded slot names, spelled once so a filter cannot disagree with the seeder. */
object Slots {
    const val LEGS = "Legs"
    const val PULL = "Pull"
    const val ROW = "Row"
    const val PUSH = "Push"
    const val OVERHEAD = "Overhead"

    /**
     * The book's Big Three: leg press, pulldown, chest press. Dropping the row and the
     * overhead press leaves the same movements covered by fewer sets.
     */
    val BIG_THREE = setOf(LEGS, PULL, PUSH)

    /** The two the cutting phase alternates between, one per session. */
    val CUTTING_UPPER = listOf(PUSH, ROW)
}

/**
 * The ladder a bodyweight exercise climbs instead of adding plates.
 *
 * On the push-up board a handle position change *is* the resistance change, and it is a much
 * coarser one than five percent — wide to standard is not a nudge. That is why advancing a
 * position asks for more evidence than a weight bump does.
 */
enum class BoardPosition(val label: String) {
    WIDE("Wide"),
    STANDARD("Standard"),
    NARROW("Narrow"),
    DECLINE("Decline");

    fun next(): BoardPosition? = entries.getOrNull(ordinal + 1)

    companion object {
        fun from(label: String?): BoardPosition? =
            entries.firstOrNull { it.label.equals(label?.trim(), ignoreCase = true) }
    }
}

/**
 * Which exercises a session actually runs.
 *
 * Every variation here is an override applied when the session is generated, never an edit to
 * the stored slot list. That matters more than it sounds: a cutting phase or a night without
 * machines is a temporary decision, and if it rewrote the configuration then turning it back
 * off would have to reconstruct what was there before — so one mistaken tap would cost the
 * setup. Reading only makes every one of these reversible by definition.
 */
object SessionPlanner {

    /** How many clear sessions at one board position earn the next one. */
    const val POSITION_CLEARANCES_REQUIRED = 2

    /**
     * @param lastCuttingUpperSlot the upper-body slot the previous cutting session trained,
     *   read from the log rather than stored, so the alternation cannot drift out of step with
     *   what actually happened.
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
     * the session rather than the calendar keeps it true as the gap widens.
     */
    fun nextCuttingUpper(lastUpperSlot: String?): String {
        val index = Slots.CUTTING_UPPER.indexOf(lastUpperSlot)
        if (index < 0) return Slots.CUTTING_UPPER.first()
        return Slots.CUTTING_UPPER[(index + 1) % Slots.CUTTING_UPPER.size]
    }

    /** Which kind to run, given the phase toggle and whether there is equipment to hand. */
    fun kindFor(cuttingPhaseActive: Boolean, hasEquipment: Boolean): SessionKind = when {
        !hasEquipment -> SessionKind.NO_EQUIPMENT
        cuttingPhaseActive -> SessionKind.CUTTING
        else -> SessionKind.STANDARD
    }

    /**
     * The position to open a bodyweight exercise at: the next one along only once the current
     * one has cleared the ceiling often enough to justify a jump that coarse.
     */
    fun openingPositionFor(lastPosition: String?, clearancesAtThisPosition: Int): String? {
        if (lastPosition == null) return null
        if (clearancesAtThisPosition < POSITION_CLEARANCES_REQUIRED) return lastPosition
        return BoardPosition.from(lastPosition)?.next()?.label ?: lastPosition
    }
}
