package com.tom.fourhourbody.domain

import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.SessionEntity
import com.tom.fourhourbody.domain.training.BoardPosition
import com.tom.fourhourbody.domain.training.StickingPointRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class StickingPointRulesTest {

    private val day = LocalDate.of(2026, 8, 1)

    private fun session(
        id: Long,
        dayOffset: Long,
        plateaued: Boolean,
        exercise: String? = null,
        completed: Boolean = true
    ) = SessionEntity(
        id = id,
        date = day.plusDays(dayOffset),
        runId = 1,
        completed = completed,
        plateaued = plateaued,
        plateauedOnExercise = exercise
    )

    private fun log(position: String?, tulSec: Int, id: Long = 0) = ExerciseLogEntity(
        id = id,
        sessionId = 1,
        exerciseName = "Wide-grip push-up",
        equipment = "Board",
        weightKg = 0.0,
        position = position,
        tulSec = tulSec
    )

    // ---- when the techniques are offered ----

    @Test
    fun `one plateau is not a sticking point`() {
        // A single plateau is how a run is meant to end. It buys a rest day and nothing else.
        val sessions = listOf(
            session(2, 7, plateaued = true, exercise = "Chest press"),
            session(1, 0, plateaued = false)
        )
        assertNull(StickingPointRules.stickingPoint(sessions))
    }

    @Test
    fun `two in a row on the same exercise is a sticking point`() {
        val sessions = listOf(
            session(3, 14, plateaued = true, exercise = "Chest press"),
            session(2, 7, plateaued = true, exercise = "Chest press"),
            session(1, 0, plateaued = false)
        )
        val stuck = StickingPointRules.stickingPoint(sessions)
        assertEquals("Chest press", stuck?.exerciseName)
        assertEquals(2, stuck?.consecutivePlateaus)
    }

    @Test
    fun `plateaus on different exercises are not a sticking point on either`() {
        // Whatever is going on is not specific to one movement, so a sticking-point technique
        // is the wrong tool for it.
        val sessions = listOf(
            session(3, 14, plateaued = true, exercise = "Chest press"),
            session(2, 7, plateaued = true, exercise = "Leg press")
        )
        assertNull(StickingPointRules.stickingPoint(sessions))
    }

    @Test
    fun `a clean session in between breaks the chain`() {
        val sessions = listOf(
            session(4, 21, plateaued = true, exercise = "Chest press"),
            session(3, 14, plateaued = false),
            session(2, 7, plateaued = true, exercise = "Chest press")
        )
        assertNull(StickingPointRules.stickingPoint(sessions))
    }

    @Test
    fun `nothing is offered once a later session has trained through it`() {
        // Having got past it means the extra rest did its job.
        val sessions = listOf(
            session(4, 21, plateaued = false),
            session(3, 14, plateaued = true, exercise = "Chest press"),
            session(2, 7, plateaued = true, exercise = "Chest press")
        )
        assertNull(StickingPointRules.stickingPoint(sessions))
    }

    @Test
    fun `an abandoned session neither counts nor breaks the chain`() {
        // It was never trained, so it says nothing either way about the exercise.
        val sessions = listOf(
            session(4, 21, plateaued = false, completed = false),
            session(3, 14, plateaued = true, exercise = "Pulldown"),
            session(2, 7, plateaued = true, exercise = "Pulldown")
        )
        assertEquals("Pulldown", StickingPointRules.stickingPoint(sessions)?.exerciseName)
    }

    @Test
    fun `the order of the input does not matter`() {
        val oldestFirst = listOf(
            session(1, 0, plateaued = false),
            session(2, 7, plateaued = true, exercise = "Chest press"),
            session(3, 14, plateaued = true, exercise = "Chest press")
        )
        assertEquals("Chest press", StickingPointRules.stickingPoint(oldestFirst)?.exerciseName)
    }

    @Test
    fun `no sessions means nothing is offered`() {
        assertNull(StickingPointRules.stickingPoint(emptyList()))
    }

    @Test
    fun `a plateaued session naming no exercise offers nothing`() {
        val sessions = listOf(session(1, 0, plateaued = true, exercise = null))
        assertNull(StickingPointRules.stickingPoint(sessions))
    }

    @Test
    fun `three in a row reports three`() {
        val sessions = (1L..3L).map {
            session(it, it * 7, plateaued = true, exercise = "Pulldown")
        }
        assertEquals(3, StickingPointRules.stickingPoint(sessions)?.consecutivePlateaus)
    }

    // ---- counting clearances for a position advance ----

    @Test
    fun `clearances count only consecutive sets at the same position`() {
        val recent = listOf(
            log(BoardPosition.WIDE.label, 100, id = 3),
            log(BoardPosition.WIDE.label, 95, id = 2),
            log(BoardPosition.WIDE.label, 80, id = 1)
        )
        assertEquals(2, StickingPointRules.clearancesAtPosition(recent, BoardPosition.WIDE.label))
    }

    @Test
    fun `a change of position restarts the count`() {
        val recent = listOf(
            log(BoardPosition.NARROW.label, 100, id = 3),
            log(BoardPosition.WIDE.label, 95, id = 2)
        )
        assertEquals(1, StickingPointRules.clearancesAtPosition(recent, BoardPosition.NARROW.label))
    }

    @Test
    fun `a set short of the ceiling counts nothing`() {
        val recent = listOf(log(BoardPosition.WIDE.label, 75, id = 1))
        assertEquals(0, StickingPointRules.clearancesAtPosition(recent, BoardPosition.WIDE.label))
    }

    @Test
    fun `exactly the ceiling counts, since that is what earns a bump elsewhere`() {
        val recent = listOf(log(BoardPosition.WIDE.label, 90, id = 1))
        assertEquals(1, StickingPointRules.clearancesAtPosition(recent, BoardPosition.WIDE.label))
    }

    @Test
    fun `blank and absent positions count as the same position`() {
        val recent = listOf(log("  ", 95, id = 2), log(null, 95, id = 1))
        assertEquals(2, StickingPointRules.clearancesAtPosition(recent, null))
    }

    @Test
    fun `no history counts nothing`() {
        assertEquals(0, StickingPointRules.clearancesAtPosition(emptyList(), "Wide"))
    }
}
