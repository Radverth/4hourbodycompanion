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
    fun `six rest days means the next session is seven days later`() {
        assertEquals(monday.plusDays(7), SessionScheduler.nextSessionDate(monday, 6))
    }

    @Test
    fun `a plateau pushes the gap out by a day`() {
        assertEquals(7, SessionScheduler.restDaysAfterPlateau(6))
        assertEquals(monday.plusDays(8), SessionScheduler.nextSessionDate(monday, 7))
    }

    @Test
    fun `nothing logged yet means a session is due`() {
        assertTrue(SessionScheduler.isSessionDue(monday, null, 6))
    }

    @Test
    fun `a session is not due until the gap has passed`() {
        assertFalse(SessionScheduler.isSessionDue(monday.plusDays(6), monday, 6))
        assertTrue(SessionScheduler.isSessionDue(monday.plusDays(7), monday, 6))
        assertTrue(SessionScheduler.isSessionDue(monday.plusDays(20), monday, 6))
    }

    @Test
    fun `days until the next session never goes negative`() {
        assertEquals(7L, SessionScheduler.daysUntilNextSession(monday, monday, 6))
        assertEquals(0L, SessionScheduler.daysUntilNextSession(monday.plusDays(20), monday, 6))
    }

    @Test
    fun `expected sessions shrink as the gap grows`() {
        assertEquals(4, SessionScheduler.expectedSessionsIn(28, 6))
        assertEquals(3, SessionScheduler.expectedSessionsIn(28, 8))
        assertEquals(2, SessionScheduler.expectedSessionsIn(28, 13))
    }
}
