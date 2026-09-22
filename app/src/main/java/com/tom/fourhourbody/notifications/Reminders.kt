package com.tom.fourhourbody.notifications

/**
 * Every scheduled reminder in the app. [requestCodeBase] keeps PendingIntent request codes
 * stable so a reminder can be cancelled precisely.
 */
enum class ReminderKind(
    val channelId: String,
    val requestCodeBase: Int,
    val title: String,
    val body: String,
    val route: String
) {
    TRAINING_SESSION(
        channelId = Channels.TRAINING,
        requestCodeBase = 1000,
        title = "Session due today",
        body = "The Big Five to failure — leg press, pulldown, seated row, chest press, " +
            "overhead press. One set each.",
        route = "training"
    ),
    SKIPPED_SESSION_NUDGE(
        channelId = Channels.TRAINING,
        requestCodeBase = 1100,
        title = "Session still open",
        body = "Today's session hasn't been logged yet.",
        route = "training"
    ),
    WEEKLY_WEIGH_IN(
        channelId = Channels.PROGRESS,
        requestCodeBase = 1200,
        title = "Weekly weigh-in",
        body = "Weight, waist and hip — same conditions as last week.",
        route = "progress"
    )
}

object Channels {
    const val TRAINING = "training"
    const val PROGRESS = "progress"
}
