package com.tom.fourhourbody.domain.adherence

import com.tom.fourhourbody.data.entity.CreatineLogEntity
import com.tom.fourhourbody.data.entity.DietDayLogEntity
import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.data.entity.SleepLogEntity
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * One pillar's completion over a window. [applicable] is the denominator the pillar is
 * honestly measured against — not always the number of days, since training happens on a gap
 * and cold exposure has a weekly target rather than a daily one.
 */
data class PillarAdherence(
    val pillar: Pillar,
    val completed: Int,
    val applicable: Int,
    val detail: String
) {
    val percent: Int
        get() = if (applicable <= 0) 0 else min(100, ((completed * 100.0) / applicable).roundToInt())
}

/**
 * Every adherence rule in one place, as pure functions, so what counts as "done" is visible
 * and adjustable rather than scattered through the UI.
 */
object AdherenceRules {

    /** Sleep needs this many of its five core checks to count the night as adherent. */
    const val SLEEP_CHECKS_REQUIRED = 4

    fun training(completedSessions: Int, expectedSessions: Int): PillarAdherence =
        PillarAdherence(
            pillar = Pillar.TRAINING,
            completed = completedSessions,
            applicable = expectedSessions,
            detail = "$completedSessions of $expectedSessions scheduled sessions"
        )

    /** A day counts when any stretch — inline or standalone — was logged on it. */
    fun stretches(daysWithAnyStretch: Int, windowDays: Int): PillarAdherence =
        PillarAdherence(
            pillar = Pillar.STRETCHES,
            completed = daysWithAnyStretch,
            applicable = windowDays,
            detail = "$daysWithAnyStretch of $windowDays days with a stretch logged"
        )

    /**
     * A cheat day is part of the plan, so a logged cheat day counts as adherent. Any other day
     * counts when all three rule flags held.
     */
    fun isDietDayCompliant(day: DietDayLogEntity): Boolean =
        day.isCheatDay || (day.avoidedWhiteCarbs && day.noLiquidCalories && day.noFruit)

    fun nutrition(days: List<DietDayLogEntity>, windowDays: Int): PillarAdherence {
        val compliant = days.count(::isDietDayCompliant)
        return PillarAdherence(
            pillar = Pillar.NUTRITION,
            completed = compliant,
            applicable = windowDays,
            detail = "$compliant of $windowDays days on plan"
        )
    }

    fun sleepChecksPassed(log: SleepLogEntity): Int = listOf(
        log.roomTempOk || log.socksUsed,
        log.darkness,
        log.noScreensBeforeBed,
        log.wineWithinLimit,
        log.consistentWakeTime
    ).count { it }

    fun isSleepNightCompliant(log: SleepLogEntity): Boolean =
        sleepChecksPassed(log) >= SLEEP_CHECKS_REQUIRED

    fun sleep(logs: List<SleepLogEntity>, windowDays: Int): PillarAdherence {
        val good = logs.count(::isSleepNightCompliant)
        return PillarAdherence(
            pillar = Pillar.SLEEP,
            completed = good,
            applicable = windowDays,
            detail = "$good of $windowDays nights hitting $SLEEP_CHECKS_REQUIRED+ checks"
        )
    }

    /**
     * Cold exposure is explicitly nice-to-have, so it is measured against a weekly target
     * rather than every day.
     */
    fun cold(sessions: Int, targetPerWeek: Int, windowDays: Int): PillarAdherence {
        val target = maxOf(1, (targetPerWeek * windowDays + 6) / 7)
        return PillarAdherence(
            pillar = Pillar.COLD,
            completed = sessions,
            applicable = target,
            detail = "$sessions of about $target sessions"
        )
    }

    /** Both doses, on a day the cycle was actually running. */
    fun creatine(logs: List<CreatineLogEntity>, cycleDaysInWindow: Int): PillarAdherence {
        val full = logs.count { it.morningTaken && it.eveningTaken }
        return PillarAdherence(
            pillar = Pillar.CREATINE,
            completed = full,
            applicable = cycleDaysInWindow,
            detail = if (cycleDaysInWindow == 0) {
                "No cycle running"
            } else {
                "$full of $cycleDaysInWindow days fully dosed"
            }
        )
    }
}
