package com.tom.fourhourbody.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class ReminderTimesTest {

    private val wednesdayMorning = LocalDateTime.of(2026, 6, 10, 8, 0)

    @Test
    fun `a daily time later today fires today`() {
        val next = ReminderTimes.nextDaily(wednesdayMorning, 18 * 60)
        assertEquals(LocalDateTime.of(2026, 6, 10, 18, 0), next)
    }

    @Test
    fun `a daily time already past rolls to tomorrow`() {
        val next = ReminderTimes.nextDaily(wednesdayMorning, 7 * 60)
        assertEquals(LocalDateTime.of(2026, 6, 11, 7, 0), next)
    }

    @Test
    fun `a weekly reminder lands on the right weekday`() {
        val next = ReminderTimes.nextWeekly(wednesdayMorning, DayOfWeek.SATURDAY, 9 * 60)
        assertEquals(LocalDateTime.of(2026, 6, 13, 9, 0), next)
        assertEquals(DayOfWeek.SATURDAY, next.dayOfWeek)
    }

    @Test
    fun `a weekly reminder for today but already past waits a week`() {
        val next = ReminderTimes.nextWeekly(wednesdayMorning, DayOfWeek.WEDNESDAY, 7 * 60)
        assertEquals(LocalDateTime.of(2026, 6, 17, 7, 0), next)
    }

    @Test
    fun `the skipped-session nudge never lands past bedtime`() {
        assertEquals(21 * 60, ReminderTimes.nudgeMinutes(18 * 60))
        assertEquals(21 * 60 + 30, ReminderTimes.nudgeMinutes(20 * 60))
    }
}
