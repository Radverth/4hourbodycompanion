package com.tom.fourhourbody.notifications

import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.domain.shift.ShiftPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderTimesTest {

    private val wednesdayMorning = LocalDateTime.of(2026, 6, 10, 8, 0)

    /** No rota: the plain behaviour, unaffected by working hours. */
    private val noShift = ShiftPattern(enabled = false, anchorMonday = LocalDate.of(2026, 6, 8))

    /** Monday 8 June 2026 starts an early (8–5) week; the next week is late (9–6). */
    private val rota = ShiftPattern(anchorMonday = LocalDate.of(2026, 6, 8))

    @Test
    fun `a daily time later today fires today`() {
        val next = ReminderTimes.nextDaily(wednesdayMorning, 18 * 60, noShift)
        assertEquals(LocalDateTime.of(2026, 6, 10, 18, 0), next)
    }

    @Test
    fun `a daily time already past rolls to tomorrow`() {
        val next = ReminderTimes.nextDaily(wednesdayMorning, 7 * 60, noShift)
        assertEquals(LocalDateTime.of(2026, 6, 11, 7, 0), next)
    }

    @Test
    fun `a weekly reminder lands on the right weekday`() {
        val next = ReminderTimes.nextWeekly(wednesdayMorning, DayOfWeek.SATURDAY, 9 * 60, noShift)
        assertEquals(LocalDateTime.of(2026, 6, 13, 9, 0), next)
        assertEquals(DayOfWeek.SATURDAY, next.dayOfWeek)
    }

    @Test
    fun `a weekly reminder for today but already past waits a week`() {
        val next = ReminderTimes.nextWeekly(wednesdayMorning, DayOfWeek.WEDNESDAY, 7 * 60, noShift)
        assertEquals(LocalDateTime.of(2026, 6, 17, 7, 0), next)
    }

    @Test
    fun `desk reset ticks stay inside the work-hours window`() {
        val settings = SettingsEntity(
            deskResetStartMinutes = 9 * 60,
            deskResetEndMinutes = 17 * 60,
            deskResetIntervalHours = 3
        )
        val ticks = ReminderTimes.nextDeskResetTicks(wednesdayMorning, settings)

        assertEquals(
            listOf(
                LocalDateTime.of(2026, 6, 10, 12, 0),
                LocalDateTime.of(2026, 6, 10, 15, 0)
            ),
            ticks
        )
    }

    @Test
    fun `desk reset skips days that are not work days`() {
        val settings = SettingsEntity(workDays = setOf(DayOfWeek.MONDAY))
        val ticks = ReminderTimes.nextDeskResetTicks(wednesdayMorning, settings)

        assertTrue(ticks.isNotEmpty())
        assertTrue(ticks.all { it.dayOfWeek == DayOfWeek.MONDAY })
    }

    @Test
    fun `a prompt inside the shift is pushed to after it`() {
        // 15:00 on an early-week Wednesday is mid-shift.
        val moved = ReminderTimes.nextDaily(wednesdayMorning, 15 * 60, rota)
        assertEquals(LocalDateTime.of(2026, 6, 10, 17, 15), moved)
    }

    @Test
    fun `the six o'clock training prompt is left alone in the early week`() {
        val at = ReminderTimes.nextDaily(wednesdayMorning, 18 * 60, rota)
        assertEquals(LocalDateTime.of(2026, 6, 10, 18, 0), at)
    }

    @Test
    fun `the same prompt moves in the late week, when six o'clock is still work`() {
        val lateWeekMonday = LocalDateTime.of(2026, 6, 15, 7, 0)
        val at = ReminderTimes.nextDaily(lateWeekMonday, 17 * 60 + 30, rota)
        assertEquals(LocalDateTime.of(2026, 6, 15, 18, 15), at)
    }

    @Test
    fun `a weekly reminder that falls mid-shift moves rather than skipping the week`() {
        val at = ReminderTimes.nextWeekly(wednesdayMorning, DayOfWeek.THURSDAY, 10 * 60, rota)
        assertEquals(LocalDateTime.of(2026, 6, 11, 17, 15), at)
    }

    @Test
    fun `the desk reset waits until the shift ends when you cannot stretch at work`() {
        val settings = SettingsEntity(
            shiftAnchorMonday = LocalDate.of(2026, 6, 8),
            canStretchAtWork = false
        )
        val ticks = ReminderTimes.nextDeskResetTicks(wednesdayMorning, settings)
        assertEquals(listOf(LocalDateTime.of(2026, 6, 10, 17, 15)), ticks)
    }

    @Test
    fun `it runs through the shift instead when you can stretch at work`() {
        val settings = SettingsEntity(
            shiftAnchorMonday = LocalDate.of(2026, 6, 8),
            canStretchAtWork = true,
            deskResetIntervalHours = 3
        )
        val ticks = ReminderTimes.nextDeskResetTicks(wednesdayMorning, settings)
        assertEquals(
            listOf(
                LocalDateTime.of(2026, 6, 10, 11, 0),
                LocalDateTime.of(2026, 6, 10, 14, 0)
            ),
            ticks
        )
    }

    @Test
    fun `no desk reset is scheduled into a weekend shift that does not exist`() {
        val settings = SettingsEntity(shiftAnchorMonday = LocalDate.of(2026, 6, 8))
        val fridayEvening = LocalDateTime.of(2026, 6, 12, 20, 0)
        val ticks = ReminderTimes.nextDeskResetTicks(fridayEvening, settings)
        // Next working day is Monday of the late week, finishing at 18:00.
        assertEquals(listOf(LocalDateTime.of(2026, 6, 15, 18, 15)), ticks)
    }

    @Test
    fun `the skipped-session nudge never lands past bedtime`() {
        assertEquals(21 * 60, ReminderTimes.nudgeMinutes(SettingsEntity(reminderTimeMinutes = 18 * 60)))
        assertEquals(
            21 * 60 + 30,
            ReminderTimes.nudgeMinutes(SettingsEntity(reminderTimeMinutes = 20 * 60))
        )
    }
}
