package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.training.BoardPosition
import com.tom.fourhourbody.domain.training.ExerciseResult
import com.tom.fourhourbody.domain.training.Load
import com.tom.fourhourbody.domain.training.NextStep
import com.tom.fourhourbody.domain.training.PreviousSet
import com.tom.fourhourbody.domain.training.ProgressionEngine
import com.tom.fourhourbody.domain.training.TrainingConstants
import com.tom.fourhourbody.domain.training.TulVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {

    private fun result(
        name: String = "Chest press",
        weightKg: Double = 40.0,
        position: String? = null,
        tul: Int,
        previousTul: Int? = null,
        previousWeightKg: Double? = null,
        previousPosition: String? = null,
        isBodyweight: Boolean = false,
        clearances: Int = 1
    ) = ExerciseResult(
        exerciseName = name,
        load = Load(weightKg, position),
        tulSeconds = tul,
        previous = previousTul?.let {
            PreviousSet(Load(previousWeightKg ?: weightKg, previousPosition ?: position), it)
        },
        isBodyweight = isBodyweight,
        clearancesAtThisLoad = clearances
    )

    // ---- the window ----

    @Test
    fun `the window is bounded at sixty and ninety seconds`() {
        assertEquals(TulVerdict.BELOW_WINDOW, ProgressionEngine.verdict(59))
        assertEquals(TulVerdict.IN_WINDOW, ProgressionEngine.verdict(60))
        assertEquals(TulVerdict.IN_WINDOW, ProgressionEngine.verdict(90))
        assertEquals(TulVerdict.ABOVE_WINDOW, ProgressionEngine.verdict(91))
    }

    // ---- the stall rule, which only compares like with like ----

    @Test
    fun `failing to match the same load's last time is a stall`() {
        assertTrue(ProgressionEngine.isStall(result(tul = 70, previousTul = 75)))
    }

    @Test
    fun `matching the same load exactly is not a stall`() {
        assertFalse(ProgressionEngine.isStall(result(tul = 75, previousTul = 75)))
    }

    @Test
    fun `a shorter time on heavier weight is not a stall`() {
        // This is the condition the whole rule rests on. Adding load is *supposed* to cut the
        // time; comparing across loads would flag every successful increase as a stall, hand
        // out a rest day for it, and teach the user that progressing is a mistake.
        val afterAnIncrease = result(
            weightKg = 45.0,
            tul = 62,
            previousTul = 95,
            previousWeightKg = 40.0
        )
        assertFalse(ProgressionEngine.isStall(afterAnIncrease))
    }

    @Test
    fun `a shorter time at a different seat position is not a stall`() {
        // The book warns that an inch or two of seat difference changes the leverage, so two
        // sets at different positions are not comparable even at identical weight.
        val movedSeat = result(
            position = "4",
            tul = 70,
            previousTul = 80,
            previousPosition = "3"
        )
        assertFalse(ProgressionEngine.isStall(movedSeat))
    }

    @Test
    fun `a first attempt cannot stall`() {
        assertFalse(ProgressionEngine.isStall(result(tul = 20)))
    }

    // ---- the step ----

    @Test
    fun `clearing the window earns five to ten percent`() {
        val (low, high) = ProgressionEngine.weightStep(100.0)
        assertEquals(105.0, low, 0.001)
        assertEquals(110.0, high, 0.001)
    }

    @Test
    fun `both ends of the step round up to a half kilo`() {
        val (low, high) = ProgressionEngine.weightStep(41.0)
        assertEquals(43.5, low, 0.001)  // 43.05 rounded up
        assertEquals(45.5, high, 0.001) // 45.10 rounded up
    }

    @Test
    fun `a light load still gets a real step rather than the same weight back`() {
        // 5% of 4 kg is 0.2 kg, which rounds to 4.0 — the same weight. A suggestion of "same
        // again" is not the increase the protocol asked for, so it takes one increment.
        val (low, _) = ProgressionEngine.weightStep(4.0)
        assertTrue("expected a step above 4.0, got $low", low > 4.0)
        assertEquals(4.5, low, 0.001)
    }

    @Test
    fun `zero weight has no step to take`() {
        assertEquals(0.0 to 0.0, ProgressionEngine.weightStep(0.0))
    }

    // ---- what changes next session ----

    @Test
    fun `inside the window nothing changes`() {
        assertEquals(NextStep.Hold, ProgressionEngine.nextStepFor(result(tul = 75)))
    }

    @Test
    fun `under the window is reported and not acted on`() {
        val step = ProgressionEngine.nextStepFor(result(tul = 44))
        assertTrue(step is NextStep.BelowWindow)
        assertEquals(44, (step as NextStep.BelowWindow).tulSeconds)
    }

    @Test
    fun `clearing the window asks for more load`() {
        val step = ProgressionEngine.nextStepFor(result(weightKg = 100.0, tul = 95))
        assertTrue(step is NextStep.Heavier)
        step as NextStep.Heavier
        assertEquals(100.0, step.fromKg, 0.001)
        assertEquals(105.0, step.lowKg, 0.001)
    }

    @Test
    fun `bodyweight work advances a position only once it has cleared twice`() {
        val once = ProgressionEngine.nextStepFor(
            result(
                weightKg = 0.0,
                position = BoardPosition.WIDE.label,
                tul = 95,
                isBodyweight = true,
                clearances = 1
            )
        )
        assertEquals(NextStep.Hold, once)

        val twice = ProgressionEngine.nextStepFor(
            result(
                weightKg = 0.0,
                position = BoardPosition.WIDE.label,
                tul = 95,
                isBodyweight = true,
                clearances = 2
            )
        )
        assertTrue(twice is NextStep.NextPosition)
        twice as NextStep.NextPosition
        assertEquals(BoardPosition.WIDE.label, twice.from)
        assertEquals(BoardPosition.STANDARD.label, twice.to)
    }

    @Test
    fun `the last board position has nowhere further to go`() {
        val step = ProgressionEngine.nextStepFor(
            result(
                weightKg = 0.0,
                position = BoardPosition.DECLINE.label,
                tul = 120,
                isBodyweight = true,
                clearances = 5
            )
        )
        assertEquals(NextStep.Hold, step)
    }

    // ---- whole sessions ----

    @Test
    fun `each exercise is judged on its own time`() {
        // A real change from the previous protocol, where one short exercise held back every
        // other lift's increase. TUL is per-exercise, so the decisions are too.
        val evaluation = ProgressionEngine.evaluate(
            listOf(
                result(name = "Leg press", weightKg = 100.0, tul = 95),
                result(name = "Chest press", weightKg = 40.0, tul = 70)
            )
        )

        assertFalse(evaluation.stalled)
        assertTrue(evaluation.steps.getValue("Leg press") is NextStep.Heavier)
        // In the window, so no entry at all rather than an entry saying "no change".
        assertFalse(evaluation.steps.containsKey("Chest press"))
    }

    @Test
    fun `a stall suppresses every suggestion and names the exercise`() {
        val evaluation = ProgressionEngine.evaluate(
            listOf(
                result(name = "Leg press", weightKg = 100.0, tul = 95),
                result(name = "Chest press", weightKg = 40.0, tul = 60, previousTul = 80)
            )
        )

        assertTrue(evaluation.stalled)
        assertEquals("Chest press", evaluation.stalledOn)
        assertTrue(evaluation.steps.isEmpty())
    }

    @Test
    fun `a session totals the time under load`() {
        val evaluation = ProgressionEngine.evaluate(
            listOf(
                result(name = "Leg press", tul = 95),
                result(name = "Chest press", tul = 70)
            )
        )
        assertEquals(165, evaluation.totalTulSeconds)
    }

    @Test
    fun `an empty session concludes nothing`() {
        val evaluation = ProgressionEngine.evaluate(emptyList())
        assertFalse(evaluation.stalled)
        assertTrue(evaluation.steps.isEmpty())
        assertEquals(0, evaluation.totalTulSeconds)
    }

    // ---- the opening load ----

    @Test
    fun `the opening load repeats itself unless the window was cleared`() {
        val inWindow = PreviousSet(Load(40.0), 75)
        assertEquals(40.0, ProgressionEngine.openingLoadFor(inWindow)!!.weightKg, 0.001)

        val cleared = PreviousSet(Load(40.0), 95)
        assertEquals(42.0, ProgressionEngine.openingLoadFor(cleared)!!.weightKg, 0.001)

        assertNull(ProgressionEngine.openingLoadFor(null))
    }

    @Test
    fun `the opening position advances only with enough clearances`() {
        val cleared = PreviousSet(Load(0.0, BoardPosition.STANDARD.label), 100)
        assertEquals(
            BoardPosition.STANDARD.label,
            ProgressionEngine.openingLoadFor(cleared, isBodyweight = true, clearancesAtThisLoad = 1)!!.position
        )
        assertEquals(
            BoardPosition.NARROW.label,
            ProgressionEngine.openingLoadFor(cleared, isBodyweight = true, clearancesAtThisLoad = 2)!!.position
        )
    }

    // ---- the load identity the whole rule depends on ----

    @Test
    fun `loads match on weight and position together`() {
        assertTrue(Load(40.0).sameAs(Load(40.0)))
        assertTrue(Load(40.0, "3").sameAs(Load(40.0, " 3 ")))
        assertTrue(Load(40.0, null).sameAs(Load(40.0, "   ")))
        assertFalse(Load(40.0, "3").sameAs(Load(40.0, "4")))
        assertFalse(Load(40.0).sameAs(Load(42.0)))
    }

    @Test
    fun `the starting gap is the book's once-a-week default`() {
        assertEquals(7, TrainingConstants.INITIAL_REST_DAYS)
    }
}
