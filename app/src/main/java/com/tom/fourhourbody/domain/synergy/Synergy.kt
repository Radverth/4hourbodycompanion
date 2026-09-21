package com.tom.fourhourbody.domain.synergy

import com.tom.fourhourbody.data.entity.Pillar
import java.time.LocalDate

/**
 * A pairing the book itself makes, where doing both does more than doing either.
 *
 * Every synergy here is sourced from the protocol, not invented to fill a grid. Nothing is
 * scored or rewarded — a synergy only ever reports that two things the book pairs landed
 * together, and, more usefully, when one half has landed and the other has not yet.
 */
data class Synergy(
    val id: String,
    val name: String,
    val pillars: List<Pillar>,
    /** What makes it fire, in the user's terms. */
    val what: String,
    /** Why the book pairs them. */
    val why: String,
    /** The nudge when the first half has landed and the second is still open. */
    val prompt: String
)

/** What one day looks like to the synergy rules. Everything here is already logged. */
data class DaySignals(
    val date: LocalDate,
    val sessionCompleted: Boolean = false,
    val preWorkoutActivationInSession: Boolean = false,
    val preBedCold: Boolean = false,
    val sleepCompliant: Boolean = false,
    val isCheatDay: Boolean = false,
    val damageControlTicks: Int = 0,
    val creatineFullDose: Boolean = false
)

/** One synergy, as it stands today. */
data class SynergyState(
    val synergy: Synergy,
    val firedToday: Boolean,
    val timesInWindow: Int,
    val windowDays: Int,
    /** Set when exactly one half has landed — the half that is still open. */
    val halfOpen: Boolean
) {
    val everFired: Boolean get() = timesInWindow > 0
}

object Synergies {

    /** Glute activation is prescribed before the workout precisely to make the pressing work. */
    val PRIMED = Synergy(
        id = "primed",
        name = "Primed",
        pillars = listOf(Pillar.STRETCHES, Pillar.TRAINING),
        what = "Pre-workout glute activation, then the session it was warming up for.",
        why = "The activation work exists to wake the posterior chain before you load it. " +
            "It is the easiest part to skip and the part the pressing work depends on.",
        prompt = "You're warmed up. Finishing the session is what that was for."
    )

    /** The book's pre-bed bath is a sleep intervention as much as a cold one. */
    val SHUTDOWN = Synergy(
        id = "shutdown",
        name = "Shutdown",
        pillars = listOf(Pillar.COLD, Pillar.SLEEP),
        what = "A pre-bed cold bath, and the night that follows logged as on protocol.",
        why = "Dropping core temperature before bed is what the pre-bed bath is for. It is " +
            "a sleep tool that happens to be cold, so the two pillars are one action.",
        prompt = "Bath's done. Now the room cold and the screens off, and it counts."
    )

    /** Damage control is explicitly a stack — one measure alone is not the protocol. */
    val DAMAGE_CONTROL = Synergy(
        id = "damage_control",
        name = "Damage control",
        pillars = listOf(Pillar.NUTRITION),
        what = "Two or more damage-control measures on the cheat day.",
        why = "The protocol is a stack: citrus before, movement after, protein and fibre " +
            "first. One measure on its own is not what was tested.",
        prompt = "One measure in. The stack is where the effect is — add a second."
    )

    /** Creatine is dosed for the days you actually load, not as a daily ritual. */
    val LOADED = Synergy(
        id = "loaded",
        name = "Loaded",
        pillars = listOf(Pillar.CREATINE, Pillar.TRAINING),
        what = "Both creatine doses on a day you completed a session.",
        why = "Creatine is there to support the lifting. On a training day the dose is " +
            "doing something; the point is not to never miss a day.",
        prompt = "Session logged. The evening dose is the other half."
    )

    val ALL = listOf(PRIMED, SHUTDOWN, DAMAGE_CONTROL, LOADED)
}

object SynergyEngine {

    fun fired(synergy: Synergy, day: DaySignals): Boolean = when (synergy.id) {
        Synergies.PRIMED.id -> day.preWorkoutActivationInSession && day.sessionCompleted
        Synergies.SHUTDOWN.id -> day.preBedCold && day.sleepCompliant
        Synergies.DAMAGE_CONTROL.id -> day.isCheatDay && day.damageControlTicks >= 2
        Synergies.LOADED.id -> day.sessionCompleted && day.creatineFullDose
        else -> false
    }

    /**
     * True when one half has landed and the other has not. This is the part worth showing:
     * a synergy that has already fired needs nothing said, and one with neither half is not
     * a nudge, it is noise.
     */
    fun halfOpen(synergy: Synergy, day: DaySignals): Boolean {
        if (fired(synergy, day)) return false
        return when (synergy.id) {
            Synergies.PRIMED.id -> day.preWorkoutActivationInSession
            Synergies.SHUTDOWN.id -> day.preBedCold
            Synergies.DAMAGE_CONTROL.id -> day.isCheatDay && day.damageControlTicks == 1
            Synergies.LOADED.id -> day.sessionCompleted
            else -> false
        }
    }

    fun evaluate(
        today: DaySignals,
        window: List<DaySignals>,
        synergies: List<Synergy> = Synergies.ALL
    ): List<SynergyState> = synergies.map { synergy ->
        SynergyState(
            synergy = synergy,
            firedToday = fired(synergy, today),
            timesInWindow = window.count { fired(synergy, it) },
            windowDays = window.size,
            halfOpen = halfOpen(synergy, today)
        )
    }

    /** What the dashboard should say, if anything: the one half-open synergy worth prompting. */
    fun liveNudge(states: List<SynergyState>): SynergyState? = states.firstOrNull { it.halfOpen }
}
