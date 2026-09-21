package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.shift.ShiftPattern
import com.tom.fourhourbody.domain.shift.ShiftSchedule
import com.tom.fourhourbody.domain.shift.ShiftWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class ShiftScheduleTest {

    // Monday 7 September 2026, an early (8–5) week.
    private val anchor = LocalDate.of(2026, 9, 7)
    private val pattern = ShiftPattern(anchorMonday = anchor)

    @Test
    fun `the anchor week is the early week`() {
        assertEquals(ShiftWeek.A, ShiftSchedule.weekOf(anchor, anchor))
        assertEquals(ShiftWeek.A, ShiftSchedule.weekOf(anchor.plusDays(4), anchor))
    }

    @Test
    fun `the following week is the late week`() {
        assertEquals(ShiftWeek.B, ShiftSchedule.weekOf(anchor.plusWeeks(1), anchor))
        assertEquals(ShiftWeek.A, ShiftSchedule.weekOf(anchor.plusWeeks(2), anchor))
        assertEquals(ShiftWeek.B, ShiftSchedule.weekOf(anchor.plusWeeks(3), anchor))
    }

    @Test
    fun `weeks before the anchor alternate backwards, not into a negative modulo`() {
        assertEquals(ShiftWeek.B, ShiftSchedule.weekOf(anchor.minusWeeks(1), anchor))
        assertEquals(ShiftWeek.A, ShiftSchedule.weekOf(anchor.minusWeeks(2), anchor))
        assertEquals(ShiftWeek.B, ShiftSchedule.weekOf(anchor.minusWeeks(5), anchor))
    }

    @Test
    fun `any day of a week resolves to that week, not just its Monday`() {
        val sunday = anchor.plusWeeks(1).plusDays(6)
        assertEquals(ShiftWeek.B, ShiftSchedule.weekOf(sunday, anchor))
    }

    @Test
    fun `the early week runs eight to five`() {
        val window = ShiftSchedule.windowOn(anchor, pattern)!!
        assertEquals(8 * 60, window.first)
        assertEquals(17 * 60 - 1, window.last)
        assertEquals(9, ShiftSchedule.shiftLengthHours(anchor, pattern))
    }

    @Test
    fun `the late week runs nine to six`() {
        val monday = anchor.plusWeeks(1)
        val window = ShiftSchedule.windowOn(monday, pattern)!!
        assertEquals(9 * 60, window.first)
        assertEquals(18 * 60 - 1, window.last)
    }

    @Test
    fun `weekends are free`() {
        assertNull(ShiftSchedule.windowOn(anchor.plusDays(5), pattern))
        assertNull(ShiftSchedule.windowOn(anchor.plusDays(6), pattern))
    }

    @Test
    fun `a disabled pattern means never at work`() {
        val off = pattern.copy(enabled = false)
        assertNull(ShiftSchedule.windowOn(anchor, off))
        assertFalse(ShiftSchedule.isWorking(anchor.atTime(11, 0), off))
    }

    @Test
    fun `working hours are bounded at both ends`() {
        assertFalse(ShiftSchedule.isWorking(anchor.atTime(7, 59), pattern))
        assertTrue(ShiftSchedule.isWorking(anchor.atTime(8, 0), pattern))
        assertTrue(ShiftSchedule.isWorking(anchor.atTime(16, 59), pattern))
        // Clocking-off time is already your own.
        assertFalse(ShiftSchedule.isWorking(anchor.atTime(17, 0), pattern))
    }

    @Test
    fun `a reminder inside the shift moves to just after it`() {
        val duringEarly = anchor.atTime(15, 0)
        assertEquals(
            LocalDateTime.of(2026, 9, 7, 17, 15),
            ShiftSchedule.moveOutOfShift(duringEarly, pattern)
        )
    }

    @Test
    fun `the six o'clock training prompt survives the early week and moves in the late one`() {
        // 18:00 on an 8–5 day is already free time.
        val early = anchor.atTime(18, 0)
        assertEquals(early, ShiftSchedule.moveOutOfShift(early, pattern))

        // The same prompt on a 9–6 day lands while still at work.
        val late = anchor.plusWeeks(1).atTime(17, 30)
        assertEquals(
            LocalDateTime.of(2026, 9, 14, 18, 15),
            ShiftSchedule.moveOutOfShift(late, pattern)
        )
    }

    @Test
    fun `mornings before the shift are left alone`() {
        val earlyMorning = anchor.atTime(6, 30)
        assertEquals(earlyMorning, ShiftSchedule.moveOutOfShift(earlyMorning, pattern))
    }

    @Test
    fun `nothing moves on a day off`() {
        val saturday = anchor.plusDays(5).atTime(12, 0)
        assertEquals(saturday, ShiftSchedule.moveOutOfShift(saturday, pattern))
    }

    @Test
    fun `the desk reset lands after the shift, and not at all on a free day`() {
        assertEquals(
            LocalDateTime.of(2026, 9, 7, 17, 15),
            ShiftSchedule.afterShiftOn(anchor, pattern)
        )
        assertEquals(
            LocalDateTime.of(2026, 9, 14, 18, 15),
            ShiftSchedule.afterShiftOn(anchor.plusWeeks(1), pattern)
        )
        assertNull(ShiftSchedule.afterShiftOn(anchor.plusDays(5), pattern))
    }

    @Test
    fun `a four-day week leaves its day off free`() {
        val fourDay = pattern.copy(days = pattern.days - DayOfWeek.FRIDAY)
        assertNull(ShiftSchedule.windowOn(anchor.plusDays(4), fourDay))
        assertTrue(ShiftSchedule.isWorking(anchor.plusDays(3).atTime(10, 0), fourDay))
    }
}
