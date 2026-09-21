package com.tom.fourhourbody.data.repo

import com.tom.fourhourbody.data.entity.ColdExposureType
import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.data.entity.StretchRoutine
import com.tom.fourhourbody.domain.adherence.AdherenceRules
import com.tom.fourhourbody.domain.adherence.PillarAdherence
import com.tom.fourhourbody.domain.creatine.CreatineCycle
import com.tom.fourhourbody.domain.synergy.DaySignals
import com.tom.fourhourbody.domain.synergy.SynergyEngine
import com.tom.fourhourbody.domain.synergy.SynergyState
import com.tom.fourhourbody.domain.training.SessionScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class TrainingToday(
    val dueToday: Boolean,
    val completedToday: Boolean,
    val lastSessionStalled: Boolean,
    val restDaysBetween: Int,
    val nextSessionDate: LocalDate?
)

data class StretchesToday(
    val inlineDoneThisSession: Boolean,
    val deskResetCountToday: Int,
    val mobilityThisWeek: Int
)

data class NutritionToday(
    val dayLogged: Boolean,
    val isCheatDay: Boolean,
    val rulesMet: Int,
    val damageControlTicks: Int
)

data class SleepToday(val nightDate: LocalDate, val logged: Boolean, val checksPassed: Int)

data class ColdToday(val countThisWeek: Int)

data class CreatineToday(
    val cycleDay: Int?,
    val cycleComplete: Boolean,
    val morningTaken: Boolean,
    val eveningTaken: Boolean
)

data class DashboardState(
    val date: LocalDate,
    val settings: SettingsEntity,
    val training: TrainingToday,
    val stretches: StretchesToday,
    val nutrition: NutritionToday,
    val sleep: SleepToday,
    val cold: ColdToday,
    val creatine: CreatineToday
)

/**
 * Everything the Today screen needs, as one Flow of local queries. This is the screen opened
 * most often, so nothing here does I/O beyond Room.
 */
class DashboardRepository(
    private val settingsRepository: SettingsRepository,
    private val trainingRepository: TrainingRepository,
    private val stretchRepository: StretchRepository,
    private val nutritionRepository: NutritionRepository,
    private val sleepRepository: SleepRepository,
    private val coldRepository: ColdRepository,
    private val creatineRepository: CreatineRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun today(date: LocalDate, nightDate: LocalDate): Flow<DashboardState> {
        val weekStart = date.minusDays(6)

        val trainingFlow = combine(
            trainingRepository.schedule(date),
            trainingRepository.observeSessionsOn(date)
        ) { schedule, todaysSessions ->
            TrainingToday(
                dueToday = schedule.dueToday && todaysSessions.none { it.completed },
                completedToday = todaysSessions.any { it.completed },
                lastSessionStalled = todaysSessions.firstOrNull { it.completed }?.stalled == true,
                restDaysBetween = schedule.restDaysBetween,
                nextSessionDate = schedule.nextSessionDate
            )
        }

        val stretchFlow = combine(
            stretchRepository.logsOn(date),
            stretchRepository.countStandaloneBetween(
                StretchRoutine.REST_DAY_MOBILITY,
                weekStart,
                date
            )
        ) { todaysLogs, mobilityThisWeek ->
            StretchesToday(
                inlineDoneThisSession = todaysLogs.any { it.sessionId != null },
                deskResetCountToday = todaysLogs.count { it.routine == StretchRoutine.DESK_RESET },
                mobilityThisWeek = mobilityThisWeek
            )
        }

        val nutritionFlow = nutritionRepository.observeDay(date).flatMapLatest { day ->
            if (day == null) {
                flowOf(NutritionToday(false, false, 0, 0))
            } else {
                nutritionRepository.observeDamageControl(day.id).map { damage ->
                    NutritionToday(
                        dayLogged = true,
                        isCheatDay = day.isCheatDay,
                        rulesMet = listOf(
                            day.avoidedWhiteCarbs,
                            day.noLiquidCalories,
                            day.noFruit
                        ).count { it },
                        damageControlTicks = damage?.let {
                            listOf(
                                it.proteinFiberFirstMeal,
                                it.citrusBeforeBigMeal,
                                it.movementBeforeMeal,
                                it.movementAfterMeal,
                                it.walkedAfterMeal
                            ).count { tick -> tick }
                        } ?: 0
                    )
                }
            }
        }

        val sleepFlow = sleepRepository.observe(nightDate).map { log ->
            SleepToday(
                nightDate = nightDate,
                logged = log != null,
                checksPassed = log?.let(AdherenceRules::sleepChecksPassed) ?: 0
            )
        }

        val coldFlow = coldRepository.countBetween(weekStart, date).map(::ColdToday)

        val creatineFlow = combine(
            settingsRepository.settings,
            creatineRepository.observe(date)
        ) { settings, log ->
            val state = CreatineCycle.stateOn(settings.creatineCycleStartDate, date)
            CreatineToday(
                cycleDay = state.day,
                cycleComplete = state.complete,
                morningTaken = log?.morningTaken == true,
                eveningTaken = log?.eveningTaken == true
            )
        }

        val lifestyle = combine(sleepFlow, coldFlow, creatineFlow, ::Triple)

        return combine(
            settingsRepository.settings,
            trainingFlow,
            stretchFlow,
            nutritionFlow,
            lifestyle
        ) { settings, training, stretches, nutrition, (sleep, cold, creatine) ->
            DashboardState(
                date = date,
                settings = settings,
                training = training,
                stretches = stretches,
                nutrition = nutrition,
                sleep = sleep,
                cold = cold,
                creatine = creatine
            )
        }
    }

    /**
     * Per-pillar completion over the last [windowDays] days. Disabled pillars are left out
     * entirely — their history stays in the database, it just stops being scored.
     */
    /**
     * Enabled pillars only — what the Today screen and the adherence view measure.
     */
    fun adherence(today: LocalDate, windowDays: Int): Flow<List<PillarAdherence>> =
        adherenceAll(today, windowDays).map { (settings, all) ->
            all.filter { settings.isEnabled(it.pillar) }
        }

    /**
     * Every pillar, switched on or not, with the settings that say which are. The deck needs
     * the ones that are off too — a card you are not running is still a card you own.
     */
    fun adherenceAll(
        today: LocalDate,
        windowDays: Int
    ): Flow<Pair<SettingsEntity, List<PillarAdherence>>> {
        val from = today.minusDays((windowDays - 1).toLong())

        val trainingFlow = combine(
            trainingRepository.countCompletedBetween(from, today),
            trainingRepository.schedule(today)
        ) { completed, schedule ->
            AdherenceRules.training(
                completedSessions = completed,
                expectedSessions = SessionScheduler.expectedSessionsIn(
                    windowDays,
                    schedule.restDaysBetween
                )
            )
        }

        val stretchFlow = stretchRepository.logsBetween(from, today).map { logs ->
            AdherenceRules.stretches(logs.map { it.date }.distinct().size, windowDays)
        }

        val nutritionFlow = nutritionRepository.observeDaysBetween(from, today).map { days ->
            AdherenceRules.nutrition(days, windowDays)
        }

        val sleepFlow = sleepRepository.observeBetween(from, today).map { logs ->
            AdherenceRules.sleep(logs, windowDays)
        }

        val lifestyleFlow = combine(
            settingsRepository.settings,
            coldRepository.countBetween(from, today),
            creatineRepository.observeBetween(from, today)
        ) { settings, coldCount, creatineLogs ->
            val coldTarget = if (settings.coldRemindersEnabled) {
                maxOf(1, settings.coldReminderDays.size)
            } else {
                2
            }
            val cycleDays = (0 until windowDays).count { offset ->
                CreatineCycle.stateOn(
                    settings.creatineCycleStartDate,
                    from.plusDays(offset.toLong())
                ).day != null
            }
            settings to listOf(
                AdherenceRules.cold(coldCount, coldTarget, windowDays),
                AdherenceRules.creatine(creatineLogs, cycleDays)
            )
        }

        return combine(
            trainingFlow,
            stretchFlow,
            nutritionFlow,
            sleepFlow,
            lifestyleFlow
        ) { training, stretches, nutrition, sleep, (settings, lifestyle) ->
            val byPillar = (listOf(training, stretches, nutrition, sleep) + lifestyle)
                .associateBy { it.pillar }
            settings to Pillar.entries.mapNotNull { byPillar[it] }
        }
    }

    /**
     * The book's own pairings, over the same window as adherence.
     *
     * Every signal here is read back out of what was already logged — a synergy reports that
     * two things landed together, it never grants anything. The useful half is [halfOpen]:
     * when one side has landed and the other has not, there is a specific, small thing to do.
     */
    fun synergies(today: LocalDate, windowDays: Int): Flow<List<SynergyState>> {
        val from = today.minusDays((windowDays - 1).toLong())

        val trainingFlow = combine(
            trainingRepository.sessionsBetween(from, today),
            stretchRepository.logsBetween(from, today)
        ) { sessions, stretches ->
            val completedOn = sessions.filter { it.completed }.map { it.date }.toSet()
            val activationSessions = stretches
                .filter { it.routine == StretchRoutine.PRE_WORKOUT && it.sessionId != null }
                .map { it.date }
                .toSet()
            completedOn to activationSessions
        }

        val nutritionFlow = combine(
            nutritionRepository.observeDaysBetween(from, today),
            nutritionRepository.observeDamageControlBetween(from, today)
        ) { days, damage ->
            val cheatDays = days.filter { it.isCheatDay }.map { it.date }.toSet()
            cheatDays to damage.associate { it.date to it.ticks }
        }

        val lifestyleFlow = combine(
            coldRepository.observeBetween(from, today),
            sleepRepository.observeBetween(from, today),
            creatineRepository.observeBetween(from, today)
        ) { cold, sleep, creatine ->
            Triple(
                cold.filter { it.type == ColdExposureType.PRE_BED_BATH }.map { it.date }.toSet(),
                sleep.filter(AdherenceRules::isSleepNightCompliant).map { it.date }.toSet(),
                creatine.filter { it.morningTaken && it.eveningTaken }.map { it.date }.toSet()
            )
        }

        return combine(
            trainingFlow,
            nutritionFlow,
            lifestyleFlow
        ) { (completedOn, activationOn), (cheatDays, damageTicks), lifestyle ->
            val (preBedCold, goodNights, fullDoses) = lifestyle
            val window = (0 until windowDays).map { offset ->
                val date = from.plusDays(offset.toLong())
                DaySignals(
                    date = date,
                    sessionCompleted = date in completedOn,
                    preWorkoutActivationInSession = date in activationOn,
                    preBedCold = date in preBedCold,
                    sleepCompliant = date in goodNights,
                    isCheatDay = date in cheatDays,
                    damageControlTicks = damageTicks[date] ?: 0,
                    creatineFullDose = date in fullDoses
                )
            }
            SynergyEngine.evaluate(
                today = window.last(),
                window = window
            )
        }
    }
}
