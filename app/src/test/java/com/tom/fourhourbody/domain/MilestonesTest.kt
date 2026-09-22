package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.progress.Milestones
import com.tom.fourhourbody.domain.progress.Stats
import com.tom.fourhourbody.domain.progress.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestonesTest {

    @Test
    fun `a new account is level zero and has nothing reached`() {
        val stats = Stats()
        assertEquals(0, Milestones.level(stats))
        assertTrue(Milestones.reached(stats).isEmpty())
    }

    @Test
    fun `level is the count of milestones passed, not a fitted curve`() {
        val stats = Stats(sessionsCompleted = 5, runsCompleted = 1)
        // sessions 1 and 5, plus run 1.
        assertEquals(3, Milestones.level(stats))
        assertEquals(Milestones.level(stats), Milestones.reached(stats).size)
    }

    @Test
    fun `every level corresponds to a milestone that can be named`() {
        val stats = Stats(sessionsCompleted = 40, runsCompleted = 6, totalBankedKg = 250.0)
        Milestones.reached(stats).forEach { milestone ->
            assertTrue(milestone.id, milestone.title.isNotBlank())
            assertTrue(milestone.id, milestone.detail.isNotBlank())
        }
    }

    @Test
    fun `the quest log offers one target per track and never a passed one`() {
        val stats = Stats(sessionsCompleted = 5, runsCompleted = 1)
        val quests = Milestones.questLog(stats, limit = 10)

        assertTrue(quests.none { it.reached(stats) })
        assertEquals(quests.map { it.track }.distinct().size, quests.size)
    }

    @Test
    fun `the quest log leads with whatever is closest to done`() {
        // 4 of 5 sessions is further along than 0 of 1 run.
        val stats = Stats(sessionsCompleted = 4, runsCompleted = 0)
        assertEquals(Track.SESSIONS, Milestones.questLog(stats).first().track)
    }

    @Test
    fun `progress never overfills past its target`() {
        val stats = Stats(sessionsCompleted = 900, totalBankedKg = 9_000.0)
        Milestones.ALL.forEach { milestone ->
            assertTrue(milestone.id, milestone.fraction(stats) <= 1f)
        }
    }

    @Test
    fun `a finished tracker has no next target rather than an invented one`() {
        val maxed = Stats(
            sessionsCompleted = 1_000,
            runsCompleted = 1_000,
            totalBankedKg = 10_000.0,
            bestGainOnOneLiftKg = 1_000.0
        )
        assertEquals(Milestones.ALL.size, Milestones.level(maxed))
        assertTrue(Milestones.questLog(maxed).isEmpty())
        assertNull(Milestones.nextUp(maxed))
    }

    @Test
    fun `milestone ids are unique and targets climb within each track`() {
        assertEquals(Milestones.ALL.size, Milestones.ALL.map { it.id }.distinct().size)
        Track.entries.forEach { track ->
            val targets = Milestones.ALL.filter { it.track == track }.map { it.target }
            assertEquals("$track targets out of order", targets.sorted(), targets)
        }
    }
}
