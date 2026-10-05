package com.tom.fourhourbody.domain

import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.SessionKind
import com.tom.fourhourbody.domain.training.SessionPlanner
import com.tom.fourhourbody.domain.training.Slots
import com.tom.fourhourbody.domain.training.TrainingConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPlannerTest {

    private fun machine(slot: String, name: String, order: Int) = ExerciseConfigEntity(
        id = order.toLong() + 1,
        slotName = slot,
        exerciseName = name,
        equipment = "Machine",
        orderIndex = order
    )

    private fun board(slot: String, name: String, order: Int) = ExerciseConfigEntity(
        id = order.toLong() + 1,
        slotName = slot,
        exerciseName = name,
        equipment = TrainingConstants.EQUIPMENT_BOARD,
        orderIndex = order
    )

    /** The seeded shape: Big Five on machines, then the same five slots on the board. */
    private val configs = listOf(
        machine(Slots.PULL_ROW, "Seated row", 0),
        machine(Slots.PUSH, "Chest press", 1),
        machine(Slots.PULL_LAT, "Pulldown", 2),
        machine(Slots.OVERHEAD, "Overhead press", 3),
        machine(Slots.LEGS, "Leg press", 4),
        board(Slots.PULL_ROW, "Board row", 10),
        board(Slots.PUSH, "Wide-grip push-up", 11),
        board(Slots.LEGS, "Wall sit", 12)
    )

    @Test
    fun `a standard session runs the Big Five in the book's order`() {
        val names = SessionPlanner.exercisesFor(SessionKind.STANDARD, configs).map { it.exerciseName }
        assertEquals(
            listOf("Seated row", "Chest press", "Pulldown", "Overhead press", "Leg press"),
            names
        )
    }

    @Test
    fun `board rows never appear in a standard session`() {
        val chosen = SessionPlanner.exercisesFor(SessionKind.STANDARD, configs)
        assertTrue(chosen.none { it.isBodyweight })
    }

    @Test
    fun `the Big Three toggle drops the row and the overhead press`() {
        val names = SessionPlanner
            .exercisesFor(SessionKind.STANDARD, configs, bigThreeOnly = true)
            .map { it.exerciseName }
        assertEquals(listOf("Chest press", "Pulldown", "Leg press"), names)
    }

    @Test
    fun `a no-equipment session runs only the board and bodyweight rows`() {
        val chosen = SessionPlanner.exercisesFor(SessionKind.NO_EQUIPMENT, configs)
        assertTrue(chosen.isNotEmpty())
        assertTrue(chosen.all { it.isBodyweight })
        assertEquals(listOf("Board row", "Wide-grip push-up", "Wall sit"), chosen.map { it.exerciseName })
    }

    @Test
    fun `a cutting session is the leg press plus one upper-body exercise`() {
        val chosen = SessionPlanner.exercisesFor(SessionKind.CUTTING, configs)
        assertEquals(2, chosen.size)
        assertTrue(chosen.any { it.slotName == Slots.LEGS })
    }

    @Test
    fun `cutting alternates the upper-body exercise from one session to the next`() {
        val first = SessionPlanner.exercisesFor(SessionKind.CUTTING, configs, lastCuttingUpperSlot = null)
        assertTrue(first.any { it.exerciseName == "Chest press" })

        val second = SessionPlanner.exercisesFor(
            SessionKind.CUTTING,
            configs,
            lastCuttingUpperSlot = Slots.PUSH
        )
        assertTrue(second.any { it.exerciseName == "Seated row" })

        val third = SessionPlanner.exercisesFor(
            SessionKind.CUTTING,
            configs,
            lastCuttingUpperSlot = Slots.PULL_ROW
        )
        assertTrue(third.any { it.exerciseName == "Chest press" })
    }

    @Test
    fun `an unrecognised last slot starts the alternation rather than breaking it`() {
        // The slot could be missing because the previous session was abandoned, or because the
        // user renamed a slot. Either way the next session has to pick something.
        assertEquals(Slots.PUSH, SessionPlanner.nextCuttingUpper("nonsense"))
        assertEquals(Slots.PUSH, SessionPlanner.nextCuttingUpper(null))
    }

    @Test
    fun `cutting ignores the Big Three toggle, which would leave it the same two anyway`() {
        val withToggle = SessionPlanner
            .exercisesFor(SessionKind.CUTTING, configs, bigThreeOnly = true)
            .map { it.exerciseName }
        val without = SessionPlanner
            .exercisesFor(SessionKind.CUTTING, configs, bigThreeOnly = false)
            .map { it.exerciseName }
        assertEquals(without, withToggle)
    }

    @Test
    fun `an inactive slot is left out of every session kind`() {
        val benched = configs.map {
            if (it.exerciseName == "Pulldown") it.copy(isActive = false) else it
        }
        val chosen = SessionPlanner.exercisesFor(SessionKind.STANDARD, benched)
        assertTrue(chosen.none { it.exerciseName == "Pulldown" })
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
    fun `switching kinds never changes the stored slot list`() {
        // The reversibility the overrides exist for: the planner only ever reads.
        val before = configs.map { it.copy() }
        SessionPlanner.exercisesFor(SessionKind.CUTTING, configs, bigThreeOnly = true)
        SessionPlanner.exercisesFor(SessionKind.NO_EQUIPMENT, configs)
        assertEquals(before, configs)
    }
}
