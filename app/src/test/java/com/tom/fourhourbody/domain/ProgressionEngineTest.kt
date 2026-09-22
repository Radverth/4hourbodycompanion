package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.training.ExerciseResult
import com.tom.fourhourbody.domain.training.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {

    @Test
    fun `progression takes five percent, rounded up to the nearest half kg`() {
        // 40 kg + 5% = 42 kg exactly.
        assertEquals(42.0, ProgressionEngine.suggestNextWeight(40.0), 0.001)
        // 41 kg + 5% = 43.05 kg, rounds up to 43.5.
        assertEquals(43.5, ProgressionEngine.suggestNextWeight(41.0), 0.001)
    }

    @Test
    fun `a set past the ceiling earns a bump, one under it does not`() {
        val evaluation = ProgressionEngine.evaluate(
            listOf(
                ExerciseResult("Leg press", weightKg = 100.0, tulSec = 95),
                ExerciseResult("Chest press", weightKg = 40.0, tulSec = 60)
            )
        )

        assertFalse(evaluation.plateaued)
        assertNull(evaluation.plateauedOn)
        assertEquals(105.0, evaluation.nextWeights.getValue("Leg press"), 0.001)
        assertFalse(evaluation.nextWeights.containsKey("Chest press"))
    }

    @Test
    fun `same or less weight with a worse time under load is a plateau`() {
        val evaluation = ProgressionEngine.evaluate(
            listOf(
                ExerciseResult(
                    "Chest press",
                    weightKg = 40.0,
                    tulSec = 50,
                    previousWeightKg = 40.0,
                    previousTulSec = 65
                )
            )
        )

        assertTrue(evaluation.plateaued)
        assertEquals("Chest press", evaluation.plateauedOn)
    }

    @Test
    fun `beating the previous time under load is not a plateau even at the same weight`() {
        val result = ExerciseResult(
            "Chest press",
            weightKg = 40.0,
            tulSec = 70,
            previousWeightKg = 40.0,
            previousTulSec = 65
        )
        assertFalse(result.isPlateau)
    }

    @Test
    fun `more weight than last time is never a plateau, even with a shorter time under load`() {
        // Heavier weight legitimately buys a shorter time to failure — that is progress, not
        // a plateau, so the weight-increase case is excluded outright.
        val result = ExerciseResult(
            "Chest press",
            weightKg = 42.0,
            tulSec = 50,
            previousWeightKg = 40.0,
            previousTulSec = 65
        )
        assertFalse(result.isPlateau)
    }

    @Test
    fun `an exercise with no previous result cannot plateau`() {
        val result = ExerciseResult("Chest press", weightKg = 40.0, tulSec = 20)
        assertFalse(result.isPlateau)
    }

    @Test
    fun `opening weight steps up only after crossing the ceiling`() {
        assertEquals(105.0, ProgressionEngine.openingWeightFor(100.0, 92)!!, 0.001)
        assertEquals(100.0, ProgressionEngine.openingWeightFor(100.0, 60)!!, 0.001)
        assertNull(ProgressionEngine.openingWeightFor(null, null))
    }
}
