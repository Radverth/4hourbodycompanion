package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.training.ExerciseResult
import com.tom.fourhourbody.domain.training.ProgressionEngine
import com.tom.fourhourbody.domain.training.TrainingConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {

    @Test
    fun `hitting the target is not a stall`() {
        assertFalse(ProgressionEngine.isStall(reps = 7, targetReps = 7))
        assertFalse(ProgressionEngine.isStall(reps = 9, targetReps = 7))
    }

    @Test
    fun `one rep short is not a stall`() {
        assertFalse(ProgressionEngine.isStall(reps = 6, targetReps = 7))
        assertFalse(ProgressionEngine.isStall(reps = 9, targetReps = 10))
    }

    @Test
    fun `more than one rep short is a stall`() {
        assertTrue(ProgressionEngine.isStall(reps = 5, targetReps = 7))
        assertTrue(ProgressionEngine.isStall(reps = 8, targetReps = 10))
    }

    @Test
    fun `progression takes ten pounds when ten percent is smaller`() {
        // 40 kg: +10 lb is 4.54 kg, +10% is 4.0 kg, so the pound step wins.
        val next = ProgressionEngine.suggestNextWeight(40.0)
        assertEquals(45.0, next, 0.001)
    }

    @Test
    fun `progression takes ten percent when it is larger`() {
        // 100 kg: +10% is 10 kg, well past the 4.54 kg pound step.
        val next = ProgressionEngine.suggestNextWeight(100.0)
        assertEquals(110.0, next, 0.001)
    }

    @Test
    fun `progression rounds up so the step is never undercut`() {
        val next = ProgressionEngine.suggestNextWeight(41.0)
        assertTrue(next >= 41.0 + TrainingConstants.TEN_POUNDS_KG)
        assertEquals(46.0, next, 0.001)
    }

    @Test
    fun `a session where everything hits suggests an increase for each exercise`() {
        val evaluation = ProgressionEngine.evaluate(
            listOf(
                ExerciseResult("Leg press", 100.0, 10, 10),
                ExerciseResult("Chest press", 40.0, 8, 7)
            )
        )

        assertFalse(evaluation.stalled)
        assertNull(evaluation.stalledOn)
        assertEquals(110.0, evaluation.nextWeights.getValue("Leg press"), 0.001)
        assertEquals(45.0, evaluation.nextWeights.getValue("Chest press"), 0.001)
    }

    @Test
    fun `a stall suppresses every suggestion and names the exercise`() {
        val evaluation = ProgressionEngine.evaluate(
            listOf(
                ExerciseResult("Leg press", 100.0, 10, 10),
                ExerciseResult("Chest press", 40.0, 4, 7)
            )
        )

        assertTrue(evaluation.stalled)
        assertEquals("Chest press", evaluation.stalledOn)
        assertTrue(evaluation.nextWeights.isEmpty())
    }

    @Test
    fun `one rep short holds the weight rather than advancing it`() {
        val evaluation = ProgressionEngine.evaluate(
            listOf(ExerciseResult("Chest press", 40.0, 6, 7))
        )

        assertFalse(evaluation.stalled)
        assertTrue(evaluation.nextWeights.isEmpty())
    }

    @Test
    fun `opening weight steps up only after a hit`() {
        assertEquals(
            45.0,
            ProgressionEngine.openingWeightFor(40.0, 7, 7)!!,
            0.001
        )
        assertEquals(
            40.0,
            ProgressionEngine.openingWeightFor(40.0, 6, 7)!!,
            0.001
        )
        assertNull(ProgressionEngine.openingWeightFor(null, null, 7))
    }
}
