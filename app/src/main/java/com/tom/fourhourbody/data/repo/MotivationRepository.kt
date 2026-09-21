package com.tom.fourhourbody.data.repo

import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.domain.adherence.AdherenceRules
import com.tom.fourhourbody.domain.creatine.CreatineCycle
import com.tom.fourhourbody.domain.streak.Chain
import com.tom.fourhourbody.domain.streak.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.tom.fourhourbody.domain.shift.ShiftSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class MotivationState(
    /** Days where everything that applied got done. What the chain pill shows. */
    val headline: Chain,
    val nutrition: Chain,
    val sleep: Chain,
    val creatine: Chain,
    /** 0 when the cheat day is today. */
    val cheatDayIn: Int,
    val cheatDayName: String,
    val trainingIntention: String?,
    /** True while the office day is still running — nothing physical is askable yet. */
    val onShiftNow: Boolean,
    /** Minutes-of-day the current shift ends, for "free after 17:00". */
    val shiftEndsAtMinutes: Int?
)

/**
 * The behavioural layer: chains, the cheat-day countdown, and the intention the user set for
 * themselves. None of this changes what is logged — it changes what gets shown back, and
 * when.
 */
class MotivationRepository(
    private val settingsRepository: SettingsRepository,
    private val nutritionRepository: NutritionRepository,
    private val sleepRepository: SleepRepository,
    private val creatineRepository: CreatineRepository
) {

    /** Long enough for a personal best to mean something without scanning all history. */
    private val windowDays = 60

    fun state(today: LocalDate): Flow<MotivationState> {
        val from = today.minusDays((windowDays - 1).toLong())

        return combine(
            settingsRepository.settings,
            nutritionRepository.observeDaysBetween(from, today),
            sleepRepository.observeBetween(from, today),
            creatineRepository.observeBetween(from, today)
        ) { settings, dietDays, sleepLogs, creatineLogs ->
            val dietByDate = dietDays.associateBy { it.date }
            val sleepByDate = sleepLogs.associateBy { it.date }
            val creatineByDate = creatineLogs.associateBy { it.date }

            // Index 0 is today, walking backwards — the order StreakCalculator expects.
            val dates = (0 until windowDays).map { today.minusDays(it.toLong()) }

            val nutritionMet = dates.map { date ->
                dietByDate[date]?.let(AdherenceRules::isDietDayCompliant) ?: false
            }
            val sleepMet = dates.map { date ->
                sleepByDate[date]?.let(AdherenceRules::isSleepNightCompliant) ?: false
            }
            val creatineMet = dates.map { date ->
                creatineByDate[date]?.let { it.morningTaken && it.eveningTaken } ?: false
            }

            // A day is "on plan" when every daily pillar that applied to it was met. Training
            // is not daily, so it cannot break a daily chain — the gap between sessions is
            // the protocol working, not a miss.
            val headlineMet = dates.mapIndexed { index, date ->
                val applies = mutableListOf<Boolean>()
                if (settings.isEnabled(Pillar.NUTRITION)) applies += nutritionMet[index]
                if (settings.isEnabled(Pillar.SLEEP)) applies += sleepMet[index]
                if (settings.isEnabled(Pillar.CREATINE) &&
                    CreatineCycle.stateOn(settings.creatineCycleStartDate, date).day != null
                ) {
                    applies += creatineMet[index]
                }
                applies.isNotEmpty() && applies.all { it }
            }

            MotivationState(
                headline = StreakCalculator.chain(headlineMet),
                nutrition = StreakCalculator.chain(nutritionMet),
                sleep = StreakCalculator.chain(sleepMet),
                creatine = StreakCalculator.chain(creatineMet),
                cheatDayIn = daysUntilCheatDay(today, settings),
                cheatDayName = settings.cheatDay.displayName(),
                trainingIntention = settings.intentionFor(Pillar.TRAINING),
                onShiftNow = ShiftSchedule.isWorking(LocalDateTime.now(), settings.shiftPattern),
                shiftEndsAtMinutes = ShiftSchedule.shiftEndOn(today, settings.shiftPattern)
            )
        }
    }

    private fun daysUntilCheatDay(today: LocalDate, settings: SettingsEntity): Int {
        var candidate = today
        while (candidate.dayOfWeek != settings.cheatDay) {
            candidate = candidate.plusDays(1)
        }
        return ChronoUnit.DAYS.between(today, candidate).toInt()
    }
}

private fun java.time.DayOfWeek.displayName(): String =
    getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
