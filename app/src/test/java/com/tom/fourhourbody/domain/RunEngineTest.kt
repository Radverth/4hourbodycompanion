package com.tom.fourhourbody.domain

import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.RunEnd
import com.tom.fourhourbody.data.entity.RunEntity
import com.tom.fourhourbody.data.entity.SessionEntity
import com.tom.fourhourbody.domain.run.RunEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RunEngineTest {

    private val start = LocalDate.of(2026, 7, 1)

    private val run = RunEntity(
        id = 1,
        runNumber = 3,
        startDate = start,
        endDate = start.plusDays(33),
        endedBy = RunEnd.STALL,
        restDaysAtStart = 3,
        restDaysAtEnd = 4
    )

    private fun session(id: Long, dayOffset: Long, stalled: Boolean = false) = SessionEntity(
        id = id,
        date = start.plusDays(dayOffset),
        runId = 1,
        completed = true,
        stalled = stalled
    )

    private fun log(id: Long, sessionId: Long, name: String, weight: Double, reps: Int, target: Int = 7) =
        ExerciseLogEntity(
            id = id,
            sessionId = sessionId,
            exerciseName = name,
            equipment = "Machine",
            weightKg = weight,
            reps = reps,
            targetReps = target
        )

    @Test
    fun `a run reports what each exercise gained from first session to last`() {
        val sessions = listOf(session(1, 0), session(2, 4), session(3, 9))
        val logs = listOf(
            log(1, 1, "Leg press", 100.0, 10, 10),
            log(2, 1, "Chest press", 40.0, 8),
            log(3, 2, "Leg press", 110.0, 10, 10),
            log(4, 2, "Chest press", 45.0, 7),
            log(5, 3, "Leg press", 120.0, 10, 10),
            log(6, 3, "Chest press", 49.5, 7)
        )

        val summary = RunEngine.summarise(run, sessions, logs)

        assertEquals(3, summary.sessions)
        val legPress = summary.gains.first { it.exerciseName == "Leg press" }
        assertEquals(100.0, legPress.fromKg, 0.001)
        assertEquals(120.0, legPress.toKg, 0.001)
        assertEquals(20.0, legPress.gainedKg, 0.001)
        assertEquals(29.5, summary.totalGainKg, 0.001)
    }

    @Test
    fun `gains are ordered by how much was added, biggest first`() {
        val sessions = listOf(session(1, 0), session(2, 4))
        val logs = listOf(
            log(1, 1, "Chest press", 40.0, 8),
            log(2, 1, "Leg press", 100.0, 10, 10),
            log(3, 2, "Chest press", 44.5, 7),
            log(4, 2, "Leg press", 120.0, 10, 10)
        )
        val summary = RunEngine.summarise(run, sessions, logs)
        assertEquals("Leg press", summary.gains.first().exerciseName)
    }

    @Test
    fun `the exercise that stalled is named from the session that ended the run`() {
        val sessions = listOf(session(1, 0), session(2, 4, stalled = true))
        val logs = listOf(
            log(1, 1, "Leg press", 100.0, 10, 10),
            log(2, 2, "Leg press", 110.0, 10, 10),
            // Four reps against a target of seven: more than one short, so this is the stall.
            log(3, 2, "Overhead press", 45.0, 4)
        )

        val summary = RunEngine.summarise(run, sessions, logs)
        assertEquals("Overhead press", summary.stalledOn)
    }

    @Test
    fun `one rep short does not count as the stall`() {
        val sessions = listOf(session(1, 0, stalled = true))
        val logs = listOf(log(1, 1, "Chest press", 40.0, 6))
        assertNull(RunEngine.summarise(run, sessions, logs).stalledOn)
    }

    @Test
    fun `the run records the rest days it started and ended on`() {
        val summary = RunEngine.summarise(run, emptyList(), emptyList())
        assertEquals(3, summary.restDaysBefore)
        assertEquals(4, summary.restDaysAfter)
        assertTrue(summary.isComplete)
    }

    @Test
    fun `a run still going is measured up to today and is not complete`() {
        val active = run.copy(endDate = null, endedBy = null, restDaysAtEnd = null)
        val summary = RunEngine.summarise(active, emptyList(), emptyList(), today = start.plusDays(9))
        assertEquals(10L, summary.days)
        assertFalse(summary.isComplete)
        // With no recorded end, the gap is still the one it started on.
        assertEquals(3, summary.restDaysAfter)
    }

    @Test
    fun `an abandoned session does not count towards the run`() {
        val sessions = listOf(session(1, 0), session(2, 4).copy(completed = false))
        assertEquals(1, RunEngine.summarise(run, sessions, emptyList()).sessions)
    }

    @Test
    fun `a run that only ever lost weight reports no total gain rather than a negative`() {
        val sessions = listOf(session(1, 0), session(2, 4))
        val logs = listOf(
            log(1, 1, "Chest press", 50.0, 8),
            log(2, 2, "Chest press", 45.0, 5)
        )
        val summary = RunEngine.summarise(run, sessions, logs)
        assertEquals(0.0, summary.totalGainKg, 0.001)
        assertFalse(summary.gains.first().improved)
    }

    @Test
    fun `run numbers continue from the highest already used`() {
        assertEquals(1, RunEngine.nextRunNumber(emptyList()))
        assertEquals(4, RunEngine.nextRunNumber(listOf(run, run.copy(id = 2, runNumber = 1))))
    }
}
