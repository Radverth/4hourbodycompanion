package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.creatine.CreatineCycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CreatineCycleTest {

    private val start = LocalDate.of(2026, 3, 1)

    @Test
    fun `no start date means no cycle`() {
        val state = CreatineCycle.stateOn(null, start)
        assertFalse(state.started)
        assertNull(state.day)
    }

    @Test
    fun `the first day of the cycle is day one`() {
        assertEquals(1, CreatineCycle.stateOn(start, start).day)
    }

    @Test
    fun `the cycle counts through to twenty-eight`() {
        assertEquals(14, CreatineCycle.stateOn(start, start.plusDays(13)).day)
        assertEquals(28, CreatineCycle.stateOn(start, start.plusDays(27)).day)
    }

    @Test
    fun `day twenty-nine is complete rather than day twenty-nine`() {
        val state = CreatineCycle.stateOn(start, start.plusDays(28))
        assertTrue(state.complete)
        assertNull(state.day)
    }

    @Test
    fun `a date before the start is not in the cycle`() {
        val state = CreatineCycle.stateOn(start, start.minusDays(1))
        assertFalse(state.started)
        assertNull(state.day)
    }

    @Test
    fun `restarting from a new date resets the count`() {
        val newStart = start.plusDays(40)
        assertEquals(1, CreatineCycle.stateOn(newStart, newStart).day)
        assertEquals(1, CreatineCycle.stateOn(newStart, newStart.plusDays(26)).daysRemaining)
    }

    @Test
    fun `the cycle ends twenty-seven days after it starts`() {
        assertEquals(start.plusDays(27), CreatineCycle.endDate(start))
    }
}
