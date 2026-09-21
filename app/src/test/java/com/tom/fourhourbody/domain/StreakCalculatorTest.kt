package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.streak.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakCalculatorTest {

    @Test
    fun `no history is an empty chain`() {
        val chain = StreakCalculator.chain(emptyList())
        assertEquals(0, chain.current)
        assertEquals(0, chain.best)
        assertFalse(chain.atRisk)
    }

    @Test
    fun `an unfinished today does not break the chain`() {
        // Today not logged yet, four solid days behind it.
        val chain = StreakCalculator.chain(listOf(false, true, true, true, true))
        assertEquals(4, chain.current)
        assertEquals(0, chain.passesUsed)
    }

    @Test
    fun `today counts once it is met`() {
        val chain = StreakCalculator.chain(listOf(true, true, true))
        assertEquals(3, chain.current)
    }

    @Test
    fun `one miss inside the week is absorbed by a pass`() {
        // today, yesterday met; two days ago missed; then a long run.
        val chain = StreakCalculator.chain(listOf(true, true, false, true, true, true, true))
        assertEquals(6, chain.current)
        assertEquals(1, chain.passesUsed)
        assertEquals(0, chain.passesLeft)
    }

    @Test
    fun `a second miss in the same week ends the chain`() {
        val chain = StreakCalculator.chain(listOf(true, false, true, false, true, true, true))
        assertEquals(2, chain.current)
        assertEquals(1, chain.passesUsed)
    }

    @Test
    fun `a miss older than the pass window is not forgiven`() {
        val met = listOf(true, true, true, true, true, true, true, false, true, true)
        val chain = StreakCalculator.chain(met)
        assertEquals(7, chain.current)
        assertEquals(0, chain.passesUsed)
    }

    @Test
    fun `the record ignores passes so it stays worth beating`() {
        // A forgiven chain of 6 must not claim a personal best of 6.
        val chain = StreakCalculator.chain(listOf(true, true, false, true, true, true, true))
        assertEquals(6, chain.current)
        assertEquals(6, chain.best)

        // Here the strict record from earlier history is the larger number.
        val withHistory = StreakCalculator.chain(
            listOf(true, false, false, true, true, true, true, true, true, true, true, true)
        )
        assertEquals(1, withHistory.current)
        assertEquals(9, withHistory.best)
    }

    @Test
    fun `a chain with days on it is at risk`() {
        assertTrue(StreakCalculator.chain(listOf(true, true)).atRisk)
        assertFalse(StreakCalculator.chain(listOf(false, false)).atRisk)
    }

    @Test
    fun `passes can be switched off entirely`() {
        val chain = StreakCalculator.chain(listOf(true, false, true, true), passesPerWeek = 0)
        assertEquals(1, chain.current)
        assertEquals(0, chain.passesUsed)
    }
}
