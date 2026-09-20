package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.training.SessionScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SessionSchedulerTest {

    private val monday = LocalDate.of(2026, 1, 5)

    @Test
    fun `two rest days means the next session is three days later`() {
        assertEquals(monday.plusDays(3), SessionScheduler.nextSessionDate(monday, 2))
    }

    @Test
    fun `a stall pushes the gap out by a day`() {
        assertEquals(3, SessionScheduler.restDaysAfterStall(2))
        assertEquals(monday.plusDays(4), SessionScheduler.nextSessionDate(monday, 3))
    }

    @Test
    fun `nothing logged yet means a session is due`() {
        assertTrue(SessionScheduler.isSessionDue(monday, null, 2))
    }

    @Test
    fun `a session is not due until the gap has passed`() {
        assertFalse(SessionScheduler.isSessionDue(monday.plusDays(2), monday, 2))
        assertTrue(SessionScheduler.isSessionDue(monday.plusDays(3), monday, 2))
        assertTrue(SessionScheduler.isSessionDue(monday.plusDays(9), monday, 2))
    }

    @Test
    fun `days until the next session never goes negative`() {
        assertEquals(3L, SessionScheduler.daysUntilNextSession(monday, monday, 2))
        assertEquals(0L, SessionScheduler.daysUntilNextSession(monday.plusDays(10), monday, 2))
    }

    @Test
    fun `expected sessions shrink as the gap grows`() {
        assertEquals(9, SessionScheduler.expectedSessionsIn(28, 2))
        assertEquals(7, SessionScheduler.expectedSessionsIn(28, 3))
        assertEquals(5, SessionScheduler.expectedSessionsIn(28, 4))
    }
}
