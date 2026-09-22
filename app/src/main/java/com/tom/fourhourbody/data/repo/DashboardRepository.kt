package com.tom.fourhourbody.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

data class TrainingToday(
    val dueToday: Boolean,
    val completedToday: Boolean,
    val lastSessionPlateaued: Boolean,
    val restDaysBetween: Int,
    val nextSessionDate: LocalDate?
)

data class DashboardState(
    val date: LocalDate,
    val training: TrainingToday
)

/**
 * Everything the Today screen needs, as one Flow of local queries. This is the screen opened
 * most often, so nothing here does I/O beyond Room.
 */
class DashboardRepository(private val trainingRepository: TrainingRepository) {

    fun today(date: LocalDate): Flow<DashboardState> =
        combine(
            trainingRepository.schedule(date),
            trainingRepository.observeSessionsOn(date)
        ) { schedule, todaysSessions ->
            DashboardState(
                date = date,
                training = TrainingToday(
                    dueToday = schedule.dueToday && todaysSessions.none { it.completed },
                    completedToday = todaysSessions.any { it.completed },
                    lastSessionPlateaued =
                        todaysSessions.firstOrNull { it.completed }?.plateaued == true,
                    restDaysBetween = schedule.restDaysBetween,
                    nextSessionDate = schedule.nextSessionDate
                )
            )
        }
}
