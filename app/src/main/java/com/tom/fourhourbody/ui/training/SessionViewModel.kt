package com.tom.fourhourbody.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.KettlebellRoundEntity
import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.data.entity.StretchLogEntity
import com.tom.fourhourbody.data.entity.StretchRoutine
import com.tom.fourhourbody.data.repo.SettingsRepository
import com.tom.fourhourbody.data.repo.StretchRepository
import com.tom.fourhourbody.data.repo.TrainingRepository
import com.tom.fourhourbody.domain.training.ExerciseResult
import com.tom.fourhourbody.domain.training.ProgressionEngine
import com.tom.fourhourbody.domain.training.SessionEvaluation
import com.tom.fourhourbody.domain.training.TrainingConstants
import com.tom.fourhourbody.ui.stretches.StretchCompletion
import com.tom.fourhourbody.ui.stretches.StretchStep
import com.tom.fourhourbody.ui.stretches.toSteps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Where the guided session currently is. */
sealed interface SessionStage {
    data object Loading : SessionStage

    /** One-time, dismissible form cue before the first exercise. */
    data object LockedPosition : SessionStage

    /** Pre-workout glute activation — runs through the stretch engine, not a copy of it. */
    data object GluteActivation : SessionStage

    data class Strength(val index: Int) : SessionStage
    data class Rest(val nextIndex: Int) : SessionStage

    /** A miss of more than one rep ends the session here. */
    data class Stalled(val exerciseName: String) : SessionStage

    /** Hip flexor stretch, called from the stretch pillar rather than duplicated. */
    data object KettlebellPrep : SessionStage
    data object Tabata : SessionStage
    data object Abs : SessionStage
    data class Summary(val evaluation: SessionEvaluation, val restDaysNow: Int) : SessionStage
}

data class ExercisePrompt(
    val config: ExerciseConfigEntity,
    val suggestedWeightKg: Double?,
    val lastWeightKg: Double?,
    val lastReps: Int?,
    val position: Int,
    val total: Int
)

class SessionViewModel(
    private val trainingRepository: TrainingRepository,
    private val stretchRepository: StretchRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _stage = MutableStateFlow<SessionStage>(SessionStage.Loading)
    val stage: StateFlow<SessionStage> = _stage.asStateFlow()

    private val _prompt = MutableStateFlow<ExercisePrompt?>(null)
    val prompt: StateFlow<ExercisePrompt?> = _prompt.asStateFlow()

    private val _gluteSteps = MutableStateFlow<List<StretchStep>>(emptyList())
    val gluteSteps: StateFlow<List<StretchStep>> = _gluteSteps.asStateFlow()

    private val _hipFlexorSteps = MutableStateFlow<List<StretchStep>>(emptyList())
    val hipFlexorSteps: StateFlow<List<StretchStep>> = _hipFlexorSteps.asStateFlow()

    private val _settings = MutableStateFlow(SettingsEntity())
    val settings: StateFlow<SettingsEntity> = _settings.asStateFlow()

    private var sessionId: Long = 0
    private var configs: List<ExerciseConfigEntity> = emptyList()
    private val results = mutableListOf<ExerciseResult>()
    private val today = LocalDate.now()

    /** Rest actually taken before the next exercise, carried into its log row. */
    private var pendingRestSec: Int = 0

    init {
        viewModelScope.launch {
            val loaded = settingsRepository.current()
            _settings.value = loaded
            configs = trainingRepository.activeStrengthConfigs()
            _gluteSteps.value = stretchRepository
                .configsForNow(StretchRoutine.PRE_WORKOUT)
                .flatMap { it.toSteps() }
            _hipFlexorSteps.value = stretchRepository
                .configsForNow(StretchRoutine.PRE_KETTLEBELL)
                .flatMap { it.toSteps() }

            sessionId = trainingRepository.startSession(today)

            _stage.value = if (loaded.lockedPositionCueDismissed) {
                SessionStage.GluteActivation
            } else {
                SessionStage.LockedPosition
            }
        }
    }

    fun acknowledgeLockedPosition(dontShowAgain: Boolean) {
        viewModelScope.launch {
            if (dontShowAgain) {
                settingsRepository.update { it.copy(lockedPositionCueDismissed = true) }
            }
            _stage.value = SessionStage.GluteActivation
        }
    }

    /** Inline stretch logs carry the session id — that is what separates them from standalone. */
    fun onStretchesDone(completions: List<StretchCompletion>, next: SessionStage) {
        viewModelScope.launch {
            if (completions.isNotEmpty()) {
                stretchRepository.logAll(
                    completions.map {
                        StretchLogEntity(
                            sessionId = sessionId,
                            stretchName = it.name,
                            sideOrPosition = it.sideOrPosition,
                            durationSec = it.durationSec,
                            repsCompleted = it.repsCompleted,
                            routine = it.routine,
                            date = today
                        )
                    }
                )
            }
            if (next is SessionStage.Strength) {
                if (configs.isEmpty()) {
                    finishSession()
                    return@launch
                }
                loadPrompt(next.index)
            }
            _stage.value = next
        }
    }

    private suspend fun loadPrompt(index: Int) {
        val config = configs.getOrNull(index) ?: return
        val last = trainingRepository.lastLogFor(config.exerciseName)
        _prompt.value = ExercisePrompt(
            config = config,
            suggestedWeightKg = trainingRepository.openingWeightFor(config),
            lastWeightKg = last?.weightKg,
            lastReps = last?.reps,
            position = index + 1,
            total = configs.size
        )
    }

    /**
     * Log one exercise. A miss of more than one rep stops the session here — the remaining
     * exercises are not run — and adds a rest day to every session that follows.
     */
    fun logExercise(index: Int, weightKg: Double, reps: Int) {
        val config = configs.getOrNull(index) ?: return
        viewModelScope.launch {
            trainingRepository.logExercise(
                ExerciseLogEntity(
                    sessionId = sessionId,
                    exerciseName = config.exerciseName,
                    equipment = config.equipment,
                    weightKg = weightKg,
                    reps = reps,
                    targetReps = config.targetReps,
                    tempo = "${TrainingConstants.TEMPO_UP_SEC}/${TrainingConstants.TEMPO_DOWN_SEC}",
                    restSecActual = pendingRestSec
                )
            )
            results += ExerciseResult(config.exerciseName, weightKg, reps, config.targetReps)
            pendingRestSec = 0

            when {
                ProgressionEngine.isStall(reps, config.targetReps) ->
                    _stage.value = SessionStage.Stalled(config.exerciseName)

                index == configs.lastIndex -> _stage.value = nextAfterStrength()

                else -> _stage.value = SessionStage.Rest(index + 1)
            }
        }
    }

    private fun nextAfterStrength(): SessionStage =
        if (_hipFlexorSteps.value.isNotEmpty()) SessionStage.KettlebellPrep else SessionStage.Tabata

    fun onRestFinished(actualRestSec: Int, nextIndex: Int) {
        pendingRestSec = actualRestSec
        viewModelScope.launch {
            loadPrompt(nextIndex)
            _stage.value = SessionStage.Strength(nextIndex)
        }
    }

    fun logKettlebellRound(roundNumber: Int, swingCount: Int, bellWeightKg: Double) {
        viewModelScope.launch {
            trainingRepository.logKettlebellRound(
                KettlebellRoundEntity(
                    sessionId = sessionId,
                    roundNumber = roundNumber,
                    swingCount = swingCount,
                    bellWeightKg = bellWeightKg
                )
            )
        }
    }

    fun onTabataFinished() {
        if (_settings.value.sixMinuteAbsEnabled) {
            _stage.value = SessionStage.Abs
        } else {
            finishSession()
        }
    }

    fun onAbsFinished() = finishSession()

    fun skipToFinish() = finishSession()

    /**
     * Saves the session and, when it stalled, applies the book's frequency adjustment: one more
     * rest day before the next session.
     */
    fun finishSession(notes: String? = null) {
        viewModelScope.launch {
            val evaluation = ProgressionEngine.evaluate(results)
            trainingRepository.finishSession(sessionId, evaluation.stalled, notes)
            val restDays = if (evaluation.stalled) {
                trainingRepository.applyStall(today)
            } else {
                trainingRepository.frequency().currentRestDaysBetweenSessions
            }
            _stage.value = SessionStage.Summary(evaluation, restDays)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SessionViewModel(
                    container.trainingRepository,
                    container.stretchRepository,
                    container.settingsRepository
                )
            }
        }
    }
}
