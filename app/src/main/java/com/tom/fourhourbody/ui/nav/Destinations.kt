package com.tom.fourhourbody.ui.nav

object Routes {
    const val TODAY = "today"
    const val TRAINING = "training"
    const val SESSION = "training/session"
    const val SESSION_HISTORY = "training/history"
    const val EXERCISE_CONFIG = "training/exercises"
    const val STRETCHES = "stretches"
    const val MOBILITY = "stretches/mobility"
    const val DESK_RESET = "stretches/desk"
    const val STRETCH_CONFIG = "stretches/config"
    const val NUTRITION = "nutrition"
    const val REFERENCE = "reference"
    const val SLEEP = "sleep"
    const val COLD = "cold"
    const val CREATINE = "creatine"
    const val DECK = "deck"
    const val PROGRESS = "progress"
    const val MORE = "more"
    const val SETTINGS = "settings"

    fun deskReset(weekly: Boolean) = "$DESK_RESET?weekly=$weekly"
    fun reference(docName: String) = "$REFERENCE/$docName"
}
