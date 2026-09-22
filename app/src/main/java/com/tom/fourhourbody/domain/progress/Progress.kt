package com.tom.fourhourbody.domain.progress

import kotlin.math.min

/**
 * The character sheet, and the rule that keeps it honest.
 *
 * A level here is not a curve fitted over experience points. It is a count: your level is the
 * number of milestones you have actually passed, and every one of them can be named and
 * checked against the log. That matters, because an XP bar that fills for opening the app is
 * the point where a game layer stops describing the work and starts replacing it — and a
 * tracker you can level up without training is one you stop believing.
 *
 * Nothing here is awarded. Every figure is read back out of what was logged.
 */
enum class Track(val label: String) {
    SESSIONS("Sessions"),
    RUNS("Runs"),
    BANKED("Weight banked"),
    LIFT("One lift"),
    DIET("Diet chain")
}

/** Everything the milestones are measured against, all of it read from the log. */
data class Stats(
    val sessionsCompleted: Int = 0,
    val runsCompleted: Int = 0,
    val totalBankedKg: Double = 0.0,
    val bestGainOnOneLiftKg: Double = 0.0,
    val bestDietChain: Int = 0
) {
    fun current(track: Track): Int = when (track) {
        Track.SESSIONS -> sessionsCompleted
        Track.RUNS -> runsCompleted
        Track.BANKED -> totalBankedKg.toInt()
        Track.LIFT -> bestGainOnOneLiftKg.toInt()
        Track.DIET -> bestDietChain
    }
}

/**
 * One nameable thing to reach. [detail] says why it is worth reaching rather than restating
 * the number, because a target with no reason behind it is just a chore.
 */
data class Milestone(
    val id: String,
    val track: Track,
    val target: Int,
    val title: String,
    val detail: String
) {
    fun reached(stats: Stats): Boolean = stats.current(track) >= target

    /** How far along, 0f..1f, for a progress bar that never overfills. */
    fun fraction(stats: Stats): Float =
        if (target <= 0) 1f else min(1f, stats.current(track).toFloat() / target)
}

object Milestones {

    val ALL: List<Milestone> = listOf(
        Milestone(
            "session_1", Track.SESSIONS, 1, "Turn up once",
            "The whole protocol rests on sessions happening at all."
        ),
        Milestone(
            "session_5", Track.SESSIONS, 5, "Five sessions",
            "Long enough for the weights to have moved on their own."
        ),
        Milestone(
            "session_15", Track.SESSIONS, 15, "Fifteen sessions",
            "Past the point where this is still a thing you are trying."
        ),
        Milestone(
            "session_40", Track.SESSIONS, 40, "Forty sessions",
            "At two a week that is most of a year of showing up."
        ),

        Milestone(
            "run_1", Track.RUNS, 1, "Close a run",
            "A stall is how a block is meant to end. Reaching one means you pushed until " +
                "the protocol said stop."
        ),
        Milestone(
            "run_3", Track.RUNS, 3, "Three runs closed",
            "Three full blocks, each starting on more rest than the last."
        ),
        Milestone(
            "run_6", Track.RUNS, 6, "Six runs closed",
            "The frequency mechanism has now been driving your schedule for months."
        ),

        Milestone(
            "banked_25", Track.BANKED, 25, "25 kg banked",
            "Total weight added across every run, and none of it resets."
        ),
        Milestone(
            "banked_100", Track.BANKED, 100, "100 kg banked",
            "A hundred kilos of added load that you carried onto the machines yourself."
        ),
        Milestone(
            "banked_250", Track.BANKED, 250, "250 kg banked",
            "Far past what any single session explains. This one is only reachable by " +
                "turning up repeatedly."
        ),

        Milestone(
            "lift_10", Track.LIFT, 10, "+10 kg on one lift",
            "One movement carried up by ten kilos since the first time you logged it."
        ),
        Milestone(
            "lift_30", Track.LIFT, 30, "+30 kg on one lift",
            "The kind of jump that changes which machine setting you reach for without " +
                "thinking."
        ),
        Milestone(
            "lift_60", Track.LIFT, 60, "+60 kg on one lift",
            "Occam's Protocol working exactly as written, over a long enough run of blocks."
        ),

        Milestone(
            "diet_7", Track.DIET, 7, "Seven days on plan",
            "One full week including the days it was inconvenient."
        ),
        Milestone(
            "diet_21", Track.DIET, 21, "Twenty-one days on plan",
            "Three weeks of slow-carb, cheat days included, without the chain breaking."
        ),
        Milestone(
            "diet_60", Track.DIET, 60, "Sixty days on plan",
            "At this length it has stopped being a diet you are on."
        )
    )

    /** Your level is the number of milestones passed. It means exactly that and nothing more. */
    fun level(stats: Stats): Int = ALL.count { it.reached(stats) }

    fun reached(stats: Stats): List<Milestone> = ALL.filter { it.reached(stats) }

    /**
     * The quest log: the nearest unreached milestone on each track, closest first, so what is
     * shown is always something you could plausibly finish next rather than the whole list.
     */
    fun questLog(stats: Stats, limit: Int = 4): List<Milestone> =
        Track.entries
            .mapNotNull { track -> ALL.firstOrNull { it.track == track && !it.reached(stats) } }
            .sortedByDescending { it.fraction(stats) }
            .take(limit)

    /** The single closest thing to done, for the one-line summary. */
    fun nextUp(stats: Stats): Milestone? = questLog(stats, limit = 1).firstOrNull()
}
