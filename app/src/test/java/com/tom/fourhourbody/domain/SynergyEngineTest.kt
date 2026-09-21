package com.tom.fourhourbody.domain

import com.tom.fourhourbody.domain.synergy.DaySignals
import com.tom.fourhourbody.domain.synergy.Synergies
import com.tom.fourhourbody.domain.synergy.SynergyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SynergyEngineTest {

    private val day = LocalDate.of(2026, 3, 4)

    @Test
    fun `primed needs the activation and the session it was warming up for`() {
        val primed = Synergies.PRIMED
        assertTrue(
            SynergyEngine.fired(
                primed,
                DaySignals(day, preWorkoutActivationInSession = true, sessionCompleted = true)
            )
        )
        assertFalse(
            SynergyEngine.fired(primed, DaySignals(day, preWorkoutActivationInSession = true))
        )
        assertFalse(SynergyEngine.fired(primed, DaySignals(day, sessionCompleted = true)))
    }

    @Test
    fun `shutdown pairs the pre-bed bath with the night that followed`() {
        val shutdown = Synergies.SHUTDOWN
        assertTrue(
            SynergyEngine.fired(shutdown, DaySignals(day, preBedCold = true, sleepCompliant = true))
        )
        assertFalse(SynergyEngine.fired(shutdown, DaySignals(day, sleepCompliant = true)))
    }

    @Test
    fun `damage control is a stack, so one measure is not enough`() {
        val dc = Synergies.DAMAGE_CONTROL
        assertFalse(SynergyEngine.fired(dc, DaySignals(day, isCheatDay = true, damageControlTicks = 1)))
        assertTrue(SynergyEngine.fired(dc, DaySignals(day, isCheatDay = true, damageControlTicks = 2)))
    }

    @Test
    fun `damage control does not fire off a cheat day, however many measures are ticked`() {
        assertFalse(
            SynergyEngine.fired(
                Synergies.DAMAGE_CONTROL,
                DaySignals(day, isCheatDay = false, damageControlTicks = 4)
            )
        )
    }

    @Test
    fun `loaded needs the session as well as the full dose`() {
        val loaded = Synergies.LOADED
        assertTrue(
            SynergyEngine.fired(loaded, DaySignals(day, sessionCompleted = true, creatineFullDose = true))
        )
        assertFalse(SynergyEngine.fired(loaded, DaySignals(day, creatineFullDose = true)))
    }

    @Test
    fun `a synergy that has already fired is not half open`() {
        val signals = DaySignals(day, preWorkoutActivationInSession = true, sessionCompleted = true)
        assertTrue(SynergyEngine.fired(Synergies.PRIMED, signals))
        assertFalse(SynergyEngine.halfOpen(Synergies.PRIMED, signals))
    }

    @Test
    fun `one half landed leaves the synergy half open`() {
        assertTrue(
            SynergyEngine.halfOpen(
                Synergies.PRIMED,
                DaySignals(day, preWorkoutActivationInSession = true)
            )
        )
    }

    @Test
    fun `neither half landed is not a nudge`() {
        assertFalse(SynergyEngine.halfOpen(Synergies.PRIMED, DaySignals(day)))
        assertFalse(SynergyEngine.halfOpen(Synergies.SHUTDOWN, DaySignals(day)))
        assertFalse(SynergyEngine.halfOpen(Synergies.LOADED, DaySignals(day)))
    }

    @Test
    fun `the window counts every day the synergy fired`() {
        val window = listOf(
            DaySignals(day.minusDays(2), preBedCold = true, sleepCompliant = true),
            DaySignals(day.minusDays(1), preBedCold = true),
            DaySignals(day, preBedCold = true, sleepCompliant = true)
        )
        val state = SynergyEngine
            .evaluate(window.last(), window)
            .first { it.synergy.id == Synergies.SHUTDOWN.id }

        assertEquals(2, state.timesInWindow)
        assertEquals(3, state.windowDays)
        assertTrue(state.firedToday)
        assertTrue(state.everFired)
    }

    @Test
    fun `the live nudge is the half-open synergy, and nothing when none is`() {
        val quiet = SynergyEngine.evaluate(DaySignals(day), listOf(DaySignals(day)))
        assertNull(SynergyEngine.liveNudge(quiet))

        val open = SynergyEngine.evaluate(
            DaySignals(day, sessionCompleted = true),
            listOf(DaySignals(day, sessionCompleted = true))
        )
        assertEquals(Synergies.LOADED.id, SynergyEngine.liveNudge(open)!!.synergy.id)
    }

    @Test
    fun `every synergy carries the two things it pairs and why`() {
        Synergies.ALL.forEach { synergy ->
            assertTrue(synergy.id, synergy.what.isNotBlank())
            assertTrue(synergy.id, synergy.why.isNotBlank())
            assertTrue(synergy.id, synergy.prompt.isNotBlank())
            assertTrue(synergy.id, synergy.pillars.isNotEmpty())
        }
        assertEquals(Synergies.ALL.size, Synergies.ALL.map { it.id }.distinct().size)
    }
}
