package com.tom.fourhourbody.domain

import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.SessionKind
import com.tom.fourhourbody.domain.training.BoardPosition
import com.tom.fourhourbody.domain.training.SessionPlanner
import com.tom.fourhourbody.domain.training.Slots
import com.tom.fourhourbody.domain.training.TrainingConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPlannerTest {

    private fun config(slot: String, name: String, equipment: String, order: Int) =
        ExerciseConfigEntity(
            id = order.toLong() + 1,
            slotName = slot,
            exerciseName = name,
            equipment = equipment,
            orderIndex = order
        )

    /** The seeded shape: the Big Five on machines, then the same slots on the board. */
    private val configs = listOf(
        config(Slots.LEGS, "Leg press", "Machine", 0),
        config(Slots.PULL, "Pulldown", "Machine", 1),
        config(Slots.ROW, "Seated row", "Machine", 2),
        config(Slots.PUSH, "Chest press", "Machine", 3),
        config(Slots.OVERHEAD, "Overhead press", "Machine", 4),
        config(Slots.LEGS, "Wall sit", TrainingConstants.EQUIPMENT_BODYWEIGHT, 10),
        config(Slots.PULL, "Board row (lat position)", TrainingConstants.EQUIPMENT_BOARD, 11),
        config(Slots.PUSH, "Wide-grip push-up", TrainingConstants.EQUIPMENT_BOARD, 13)
    )

    @Test
    fun `a standard session runs the five machine exercises in slot order`() {
        val names = SessionPlanner
            .exercisesFor(SessionKind.STANDARD, configs)
            .map { it.exerciseName }
        assertEquals(
            listOf("Leg press", "Pulldown", "Seated row", "Chest press", "Overhead press"),
            names
        )
    }

    @Test
    fun `board rows never appear in a standard session`() {
        assertTrue(SessionPlanner.exercisesFor(SessionKind.STANDARD, configs).none { it.isBodyweight })
    }

    @Test
    fun `the Big Three toggle drops the row and the overhead press`() {
        val names = SessionPlanner
            .exercisesFor(SessionKind.STANDARD, configs, bigThreeOnly = true)
            .map { it.exerciseName }
        assertEquals(listOf("Leg press", "Pulldown", "Chest press"), names)
    }

    @Test
    fun `a no-equipment session runs only the board and bodyweight rows`() {
        val chosen = SessionPlanner.exercisesFor(SessionKind.NO_EQUIPMENT, configs)
        assertTrue(chosen.isNotEmpty())
        assertTrue(chosen.all { it.isBodyweight })
        assertEquals(
            listOf("Wall sit", "Board row (lat position)", "Wide-grip push-up"),
            chosen.map { it.exerciseName }
        )
    }

    @Test
    fun `a cutting session is the leg press plus one upper-body exercise`() {
        val chosen = SessionPlanner.exercisesFor(SessionKind.CUTTING, configs)
        assertEquals(2, chosen.size)
        assertTrue(chosen.any { it.slotName == Slots.LEGS })
        assertTrue(chosen.none { it.isBodyweight })
    }

    @Test
    fun `cutting alternates the upper-body exercise from one session to the next`() {
        val first = SessionPlanner.exercisesFor(SessionKind.CUTTING, configs, lastCuttingUpperSlot = null)
        assertTrue(first.any { it.exerciseName == "Chest press" })

        val second = SessionPlanner.exercisesFor(
            SessionKind.CUTTING, configs, lastCuttingUpperSlot = Slots.PUSH
        )
        assertTrue(second.any { it.exerciseName == "Seated row" })

        val third = SessionPlanner.exercisesFor(
            SessionKind.CUTTING, configs, lastCuttingUpperSlot = Slots.ROW
        )
        assertTrue(third.any { it.exerciseName == "Chest press" })
    }

    @Test
    fun `an unrecognised last slot starts the alternation rather than breaking it`() {
        // The slot can be missing because the previous session was abandoned, or renamed.
        // Either way the next session still has to pick something.
        assertEquals(Slots.PUSH, SessionPlanner.nextCuttingUpper("nonsense"))
        assertEquals(Slots.PUSH, SessionPlanner.nextCuttingUpper(null))
    }

    @Test
    fun `an inactive slot is left out of every session kind`() {
        val benched = configs.map {
            if (it.exerciseName == "Pulldown") it.copy(isActive = false) else it
        }
        assertTrue(
            SessionPlanner.exercisesFor(SessionKind.STANDARD, benched)
                .none { it.exerciseName == "Pulldown" }
        )
    }

    @Test
    fun `no equipment outranks the cutting phase when choosing a kind`() {
        assertEquals(
            SessionKind.NO_EQUIPMENT,
            SessionPlanner.kindFor(cuttingPhaseActive = true, hasEquipment = false)
        )
        assertEquals(
            SessionKind.CUTTING,
            SessionPlanner.kindFor(cuttingPhaseActive = true, hasEquipment = true)
        )
        assertEquals(
            SessionKind.STANDARD,
            SessionPlanner.kindFor(cuttingPhaseActive = false, hasEquipment = true)
        )
    }

    @Test
    fun `planning never changes the stored slot list`() {
        // The reversibility the overrides exist for: the planner only ever reads.
        val before = configs.map { it.copy() }
        SessionPlanner.exercisesFor(SessionKind.CUTTING, configs, bigThreeOnly = true)
        SessionPlanner.exercisesFor(SessionKind.NO_EQUIPMENT, configs)
        assertEquals(before, configs)
    }

    // ---- the board position ladder ----

    @Test
    fun `a position advances only once it has cleared the ceiling enough times`() {
        assertEquals(
            BoardPosition.WIDE.label,
            SessionPlanner.openingPositionFor(BoardPosition.WIDE.label, clearancesAtThisPosition = 1)
        )
        assertEquals(
            BoardPosition.STANDARD.label,
            SessionPlanner.openingPositionFor(BoardPosition.WIDE.label, clearancesAtThisPosition = 2)
        )
    }

    @Test
    fun `the last position on the ladder has nowhere further to go`() {
        assertEquals(
            BoardPosition.DECLINE.label,
            SessionPlanner.openingPositionFor(BoardPosition.DECLINE.label, clearancesAtThisPosition = 9)
        )
    }

    @Test
    fun `an unknown position is kept rather than reset to the start of the ladder`() {
        // A hand-typed position is still the position that was trained; snapping it back to
        // "Wide" would silently undo real progress.
        assertEquals(
            "Feet elevated",
            SessionPlanner.openingPositionFor("Feet elevated", clearancesAtThisPosition = 5)
        )
    }

    @Test
    fun `no history means no suggested position`() {
        assertNull(SessionPlanner.openingPositionFor(null, clearancesAtThisPosition = 0))
    }

    @Test
    fun `the ladder runs wide to decline and stops`() {
        assertEquals(BoardPosition.STANDARD, BoardPosition.WIDE.next())
        assertEquals(BoardPosition.NARROW, BoardPosition.STANDARD.next())
        assertEquals(BoardPosition.DECLINE, BoardPosition.NARROW.next())
        assertNull(BoardPosition.DECLINE.next())
    }

    @Test
    fun `a position label is matched regardless of case and padding`() {
        assertEquals(BoardPosition.NARROW, BoardPosition.from("  narrow "))
        assertNull(BoardPosition.from(null))
        assertNull(BoardPosition.from("sideways"))
    }
}
