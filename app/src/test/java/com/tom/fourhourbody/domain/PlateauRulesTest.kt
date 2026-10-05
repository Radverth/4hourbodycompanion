package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.training.SessionOutcome
import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.domain.training.BoardPosition
import com.tom.fourhourbody.domain.training.Load
import com.tom.fourhourbody.domain.training.PlateauRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PlateauRulesTest {

    private val day = LocalDate.of(2026, 8, 1)

    private fun outcome(id: Long, dayOffset: Long, stalled: Boolean, exercise: String?) =
        SessionOutcome(
            sessionId = id,
            date = day.plusDays(dayOffset),
            stalled = stalled,
            lastExercise = exercise
        )

    private fun log(weight: Double, position: String?, tul: Int, id: Long = 0) =
        ExerciseLogEntity(
            id = id,
            sessionId = 1,
            exerciseName = "Chest press",
            equipment = "Machine",
            weightKg = weight,
            seatPosition = position,
            tulSeconds = tul
        )

    // ---- when the toolkit is offered ----

    @Test
    fun `one stall is not a plateau`() {
        // A single stall is how a run is meant to end. It buys a rest day and nothing else.
        val outcomes = listOf(
            outcome(2, 7, stalled = true, exercise = "Chest press"),
            outcome(1, 0, stalled = false, exercise = "Leg press")
        )
        assertNull(PlateauRules.plateau(outcomes))
    }

    @Test
    fun `two stalls in a row on the same exercise is a plateau`() {
        val outcomes = listOf(
            outcome(3, 14, stalled = true, exercise = "Chest press"),
            outcome(2, 7, stalled = true, exercise = "Chest press"),
            outcome(1, 0, stalled = false, exercise = "Leg press")
        )
        val plateau = PlateauRules.plateau(outcomes)
        assertEquals("Chest press", plateau?.exerciseName)
        assertEquals(2, plateau?.consecutiveStalls)
    }

    @Test
    fun `stalls on different exercises are not a plateau on either`() {
        // Whatever is going on is not specific to one movement, so a sticking-point
        // technique is the wrong tool for it.
        val outcomes = listOf(
            outcome(3, 14, stalled = true, exercise = "Chest press"),
            outcome(2, 7, stalled = true, exercise = "Leg press")
        )
        assertNull(PlateauRules.plateau(outcomes))
    }

    @Test
    fun `a clean session in between breaks the chain`() {
        val outcomes = listOf(
            outcome(4, 21, stalled = true, exercise = "Chest press"),
            outcome(3, 14, stalled = false, exercise = "Leg press"),
            outcome(2, 7, stalled = true, exercise = "Chest press")
        )
        assertNull(PlateauRules.plateau(outcomes))
    }

    @Test
    fun `a plateau is only reported while the most recent session is the stalled one`() {
        // Having trained through it since means the extra rest did its job.
        val outcomes = listOf(
            outcome(4, 21, stalled = false, exercise = "Chest press"),
            outcome(3, 14, stalled = true, exercise = "Chest press"),
            outcome(2, 7, stalled = true, exercise = "Chest press")
        )
        assertNull(PlateauRules.plateau(outcomes))
    }

    @Test
    fun `order of the input does not matter`() {
        val oldestFirst = listOf(
            outcome(1, 0, stalled = false, exercise = "Leg press"),
            outcome(2, 7, stalled = true, exercise = "Chest press"),
            outcome(3, 14, stalled = true, exercise = "Chest press")
        )
        assertEquals("Chest press", PlateauRules.plateau(oldestFirst)?.exerciseName)
    }

    @Test
    fun `no sessions means no plateau`() {
        assertNull(PlateauRules.plateau(emptyList()))
    }

    @Test
    fun `a stalled session with nothing logged names no exercise`() {
        val outcomes = listOf(outcome(1, 0, stalled = true, exercise = null))
        assertNull(PlateauRules.plateau(outcomes))
    }

    @Test
    fun `three in a row reports three`() {
        val outcomes = (1L..3L).map { outcome(it, it * 7, stalled = true, exercise = "Pulldown") }
        assertEquals(3, PlateauRules.plateau(outcomes)?.consecutiveStalls)
    }

    // ---- counting clearances for a position step ----

    @Test
    fun `clearances count only consecutive sessions at the same load`() {
        val recent = listOf(
            log(0.0, BoardPosition.WIDE.label, 100, id = 3),
            log(0.0, BoardPosition.WIDE.label, 95, id = 2),
            log(0.0, BoardPosition.WIDE.label, 80, id = 1)
        )
        assertEquals(2, PlateauRules.clearancesAtLoad(recent, Load(0.0, BoardPosition.WIDE.label)))
    }

    @Test
    fun `a change of position restarts the count`() {
        val recent = listOf(
            log(0.0, BoardPosition.NARROW.label, 100, id = 3),
            log(0.0, BoardPosition.WIDE.label, 95, id = 2)
        )
        assertEquals(1, PlateauRules.clearancesAtLoad(recent, Load(0.0, BoardPosition.NARROW.label)))
    }

    @Test
    fun `a set inside the window counts nothing`() {
        val recent = listOf(log(0.0, BoardPosition.WIDE.label, 75, id = 1))
        assertEquals(0, PlateauRules.clearancesAtLoad(recent, Load(0.0, BoardPosition.WIDE.label)))
    }

    @Test
    fun `no history counts nothing`() {
        assertEquals(0, PlateauRules.clearancesAtLoad(emptyList(), Load(40.0)))
    }
}
