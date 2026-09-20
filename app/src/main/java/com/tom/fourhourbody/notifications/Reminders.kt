package com.tom.fourhourbody.notifications

import com.tom.fourhourbody.data.entity.Pillar

/**
 * Every scheduled reminder in the app. [requestCodeBase] keeps PendingIntent request codes
 * stable so a reminder can be cancelled precisely when its pillar is switched off.
 */
enum class ReminderKind(
    val pillar: Pillar,
    val channelId: String,
    val requestCodeBase: Int,
    val title: String,
    val body: String,
    val route: String
) {
    TRAINING_SESSION(
        pillar = Pillar.TRAINING,
        channelId = Channels.TRAINING,
        requestCodeBase = 1000,
        title = "Session due today",
        body = "Occam's Protocol — 7+ reps (10+ leg press), 5s up / 5s down, 3 min rest.",
        route = "training"
    ),
    SKIPPED_SESSION_NUDGE(
        pillar = Pillar.TRAINING,
        channelId = Channels.TRAINING,
        requestCodeBase = 1100,
        title = "Session still open",
        body = "Today's session hasn't been logged yet.",
        route = "training"
    ),
    WEEKLY_WEIGH_IN(
        pillar = Pillar.TRAINING,
        channelId = Channels.PROGRESS,
        requestCodeBase = 1200,
        title = "Weekly weigh-in",
        body = "Weight, waist and hip — same conditions as last week.",
        route = "progress"
    ),
    SLEEP_CHECKLIST(
        pillar = Pillar.SLEEP,
        channelId = Channels.SLEEP,
        requestCodeBase = 1300,
        title = "Sleep checklist",
        body = "Room 67–70°F, dark, no screens, wine finished 4+ hours ago.",
        route = "sleep"
    ),
    DESK_RESET_INTERVAL(
        pillar = Pillar.STRETCHES,
        channelId = Channels.STRETCHES,
        requestCodeBase = 1400,
        title = "Desk reset",
        body = "Static Back, Static Extension, Shoulder Bridge — you've been sitting a while.",
        route = "stretches/desk"
    ),
    WEEKLY_DESK_RESET(
        pillar = Pillar.STRETCHES,
        channelId = Channels.STRETCHES,
        requestCodeBase = 1500,
        title = "Weekly desk reset",
        body = "The full set: Active Bridges, Supine Groin Progressive, Air Bench.",
        route = "stretches/desk?weekly=true"
    ),
    WEEKLY_MOBILITY(
        pillar = Pillar.STRETCHES,
        channelId = Channels.STRETCHES,
        requestCodeBase = 1600,
        title = "Rest-day mobility",
        body = "Super quad, pelvic symmetry, pelvis repositioning.",
        route = "stretches/mobility"
    ),
    COLD_EXPOSURE(
        pillar = Pillar.COLD,
        channelId = Channels.COLD,
        requestCodeBase = 1700,
        title = "Cold exposure",
        body = "Shower, ice pack or a pre-bed bath — whichever fits today.",
        route = "cold"
    ),
    CREATINE_MORNING(
        pillar = Pillar.CREATINE,
        channelId = Channels.CREATINE,
        requestCodeBase = 1800,
        title = "Creatine — morning",
        body = "3.5 g on waking.",
        route = "creatine"
    ),
    CREATINE_EVENING(
        pillar = Pillar.CREATINE,
        channelId = Channels.CREATINE,
        requestCodeBase = 1900,
        title = "Creatine — evening",
        body = "3.5 g before bed.",
        route = "creatine"
    );

    companion object {
        /** Desk-reset interval alarms need one code per tick within the work-hours window. */
        const val MAX_DESK_RESET_TICKS = 12
    }
}

object Channels {
    const val TRAINING = "training"
    const val STRETCHES = "stretches"
    const val NUTRITION = "nutrition"
    const val SLEEP = "sleep"
    const val COLD = "cold"
    const val CREATINE = "creatine"
    const val PROGRESS = "progress"
}
