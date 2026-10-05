package com.tom.fourhourbody.ui.nav

object Routes {
    const val TODAY = "today"
    const val TRAINING = "training"
    const val SESSION = "training/session"
    const val SESSION_HISTORY = "training/history"
    const val EXERCISE_CONFIG = "training/exercises"
    const val DECK = "deck"
    const val PROGRESS = "progress"
    const val MORE = "more"
    const val SETTINGS = "settings"

    /**
     * A no-equipment night is the same session player against different exercise rows, so it
     * is this route carrying which kind to run rather than a second screen.
     */
    fun session(kind: String) = "$SESSION?kind=$kind"
}
