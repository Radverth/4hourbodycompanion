package com.tom.fourhourbody.data.repo

import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.domain.progress.Stats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/**
 * The character sheet's inputs, every one of them counted from the log.
 *
 * It lives in its own repository rather than inside a view model because both Today and the
 * deck show the level, and a number that appears twice should be computed once.
 */
class ProgressRepository(private val trainingRepository: TrainingRepository) {

    fun stats(today: LocalDate): Flow<Stats> = combine(
        trainingRepository.sessions,
        trainingRepository.runHistory(today),
        trainingRepository.completedLogs
    ) { sessions, runs, logs ->
        val finished = runs.filter { it.isComplete }
        Stats(
            sessionsCompleted = sessions.count { it.completed },
            runsCompleted = finished.size,
            // Only closed runs count towards what is banked; a run still going could plateau
            // tomorrow and its gains are already visible on the training screen.
            totalBankedKg = finished.sumOf { it.totalGainKg },
            bestGainOnOneLiftKg = bestGainOnOneLift(logs)
        )
    }

    /**
     * The largest climb any single exercise has made from its first logged weight to its
     * latest. [TrainingRepository.completedLogs] arrives oldest first, which is what makes
     * first and last meaningful.
     */
    private fun bestGainOnOneLift(logs: List<ExerciseLogEntity>): Double =
        logs.groupBy { it.exerciseName }
            .values
            .filter { it.size >= 2 }
            .maxOfOrNull { entries -> entries.last().weightKg - entries.first().weightKg }
            ?.coerceAtLeast(0.0)
            ?: 0.0
}
