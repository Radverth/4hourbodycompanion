package com.tom.fourhourbody.data.entity

/** The six trackable pillars. Every one of them is independently toggleable in Settings. */
enum class Pillar(val label: String) {
    TRAINING("Training"),
    STRETCHES("Static stretches"),
    NUTRITION("Nutrition"),
    SLEEP("Sleep"),
    COLD("Cold exposure"),
    CREATINE("Creatine")
}

enum class DietMode { SLOW_CARB, HYBRID }

/** Which of the three book-sourced stretch routines a config row belongs to. */
enum class StretchRoutine(val label: String) {
    PRE_KETTLEBELL("Pre-kettlebell"),
    PRE_WORKOUT("Pre-workout activation"),
    REST_DAY_MOBILITY("Rest-day mobility"),
    DESK_RESET("Desk reset")
}

/**
 * Hold mode is a countdown with a single completion tone. Reps mode is a plain counter —
 * Active Bridges is rep-based and must not be forced into a time-based UI.
 */
enum class StretchMode { HOLD, REPS }

enum class ColdExposureType(val label: String) {
    SHOWER("Cold shower"),
    ICE_PACK("Ice pack"),
    PRE_BED_BATH("Pre-bed bath")
}
