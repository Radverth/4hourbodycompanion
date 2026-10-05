package com.tom.fourhourbody.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.PlateauTechnique
import com.tom.fourhourbody.data.entity.RunEnd
import com.tom.fourhourbody.data.entity.SessionKind
import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.data.entity.StretchLogEntity
import com.tom.fourhourbody.data.entity.StretchRoutine
import com.tom.fourhourbody.data.repo.SettingsRepository
import com.tom.fourhourbody.data.repo.StretchRepository
import com.tom.fourhourbody.data.repo.TrainingRepository
import com.tom.fourhourbody.domain.run.RunSummary
import com.tom.fourhourbody.domain.training.ExerciseResult
import com.tom.fourhourbody.domain.training.Load
import com.tom.fourhourbody.domain.training.Plateau
import com.tom.fourhourbody.domain.training.PreviousSet
import com.tom.fourhourbody.domain.training.ProgressionEngine
import com.tom.fourhourbody.domain.training.SessionEvaluation
import com.tom.fourhourbody.ui.stretches.StretchCompletion
import com.tom.fourhourbody.ui.stretches.StretchStep
import com.tom.fourhourbody.ui.stretches.toSteps
import com.tom.fourhourbody.util.MonotonicTimer
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Where the guided session currently is. */
sealed interface SessionStage {
    data object Loading : SessionStage

    /** One-time, dismissible form cue before the first exercise. */
    data object LockedPosition : SessionStage

    /** Pre-workout glute activation — runs through the stretch engine, not a copy of it. */
    data object GluteActivation : SessionStage

    data class Strength(val index: Int) : SessionStage
    data class Rest(val nextIndex: Int) : SessionStage

    /**
     * A set that failed to match the time it held last session at the same load ends the
     * session — and with it the run — here.
     */
    data class Stalled(
        val exerciseName: String,
        val runNumber: Int,
        val tulSeconds: Int,
        val previousTulSeconds: Int,
        /** Non-null only when this is the second stall in a row on this exercise. */
        val plateau: Plateau?
    ) : SessionStage

    data class Summary(
        val evaluation: SessionEvaluation,
        val restDaysNow: Int,
        val elapsedSessionTimeSec: Int,
        val run: RunProgress
    ) : SessionStage
}

/**
 * Where this session sat in its run, and — when the session's stall closed the run — what
 * the whole run earned.
 */
data class RunProgress(
    val runNumber: Int,
    val sessionsThisRun: Int,
    val completed: RunSummary?
)

data class ExercisePrompt(
    val config: ExerciseConfigEntity,
    val suggestedLoad: Load?,
    val previous: PreviousSet?,
    /** Longest this exercise has ever been held — what a set has to beat to be a record. */
    val bestEverTulSec: Int?,
    val position: Int,
    val total: Int
) {
    val isBodyweight: Boolean get() = config.isBodyweight

    /** Called while the set is still live, not minutes later in the summary. */
    fun isRecord(tulSeconds: Int): Boolean = bestEverTulSec != null && tulSeconds > bestEverTulSec
}

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

    private val _settings = MutableStateFlow(SettingsEntity())
    val settings: StateFlow<SettingsEntity> = _settings.asStateFlow()

    private val _kind = MutableStateFlow(SessionKind.STANDARD)
    val kind: StateFlow<SessionKind> = _kind.asStateFlow()

    private var sessionId: Long = 0
    private var configs: List<ExerciseConfigEntity> = emptyList()
    private val results = mutableListOf<ExerciseResult>()
    private val today = LocalDate.now()

    /**
     * Wall-clock start, from the monotonic clock. Worth having beside summed TUL: if the
     * total creeps up while the time under load doesn't, the rests have quietly got longer.
     */
    private val startedAtMs = MonotonicTimer.now()

    /** Rest actually taken before the next exercise, carried into its log row. */
    private var pendingRestSec: Int = 0

    fun start(requestedKind: SessionKind) {
        viewModelScope.launch {
            val loaded = settingsRepository.current()
            _settings.value = loaded
            val frequency = trainingRepository.frequency()
            val kind = if (requestedKind == SessionKind.STANDARD && frequency.cuttingPhaseActive) {
                SessionKind.CUTTING
            } else {
                requestedKind
            }
            _kind.value = kind
            configs = trainingRepository.exercisesFor(kind, loaded.bigThreeOnly)
            _gluteSteps.value = stretchRepository
                .configsForNow(StretchRoutine.PRE_WORKOUT)
                .flatMap { it.toSteps() }

            sessionId = trainingRepository.startSession(today, kind)

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
        _prompt.value = ExercisePrompt(
            config = config,
            suggestedLoad = trainingRepository.openingLoadFor(config),
            previous = trainingRepository.lastSetFor(config.exerciseName),
            bestEverTulSec = trainingRepository.bestTulFor(config.exerciseName),
            position = index + 1,
            total = configs.size
        )
    }

    /**
     * Log one exercise.
     *
     * A set that fell short of the time the same load held last session stops the session
     * here — the remaining exercises are not run — and adds a rest day to every session that
     * follows. The brief is explicit that the stall ends the session, not just the exercise.
     */
    fun logExercise(index: Int, weightKg: Double, position: String?, tulSeconds: Int, reps: Int) {
        val config = configs.getOrNull(index) ?: return
        viewModelScope.launch {
            val load = Load(weightKg, position)
            val previous = trainingRepository.lastSetFor(config.exerciseName)
            val cadence = "${_settings.value.tempoUpSec}/${_settings.value.tempoDownSec}"

            trainingRepository.logExercise(
                ExerciseLogEntity(
                    sessionId = sessionId,
                    exerciseName = config.exerciseName,
                    equipment = config.equipment,
                    weightKg = weightKg,
                    seatPosition = position?.takeIf { it.isNotBlank() },
                    tulSeconds = tulSeconds,
                    reps = reps,
                    targetTulMinSec = config.targetTulMinSec,
                    targetTulMaxSec = config.targetTulMaxSec,
                    repCadenceSec = cadence,
                    restSecActual = pendingRestSec,
                    tulDerived = false
                )
            )

            val result = ExerciseResult(
                exerciseName = config.exerciseName,
                load = load,
                tulSeconds = tulSeconds,
                previous = previous,
                targetMinSec = config.targetTulMinSec,
                targetMaxSec = config.targetTulMaxSec,
                isBodyweight = config.isBodyweight,
                clearancesAtThisLoad = trainingRepository.clearancesAtLoad(
                    exerciseName = config.exerciseName,
                    load = load,
                    targetMaxSec = config.targetTulMaxSec
                )
            )
            results += result
            pendingRestSec = 0

            when {
                ProgressionEngine.isStall(result) -> {
                    // Saved before the plateau is read, so this stall counts toward it.
                    finishAndStall(result, previous)
                }

                index == configs.lastIndex -> finishSession()

                else -> _stage.value = SessionStage.Rest(index + 1)
            }
        }
    }

    private suspend fun finishAndStall(result: ExerciseResult, previous: PreviousSet?) {
        val runNumber = trainingRepository.currentRun(today).runNumber
        trainingRepository.finishSession(
            sessionId = sessionId,
            stalled = true,
            elapsedSessionTimeSec = elapsedSec(),
            notes = null
        )
        _stage.value = SessionStage.Stalled(
            exerciseName = result.exerciseName,
            runNumber = runNumber,
            tulSeconds = result.tulSeconds,
            previousTulSeconds = previous?.tulSeconds ?: 0,
            plateau = trainingRepository.plateau()?.takeIf { it.exerciseName == result.exerciseName }
        )
    }

    fun onRestFinished(actualRestSec: Int, nextIndex: Int) {
        pendingRestSec = actualRestSec
        viewModelScope.launch {
            loadPrompt(nextIndex)
            _stage.value = SessionStage.Strength(nextIndex)
        }
    }

    fun logPlateauTechnique(exerciseName: String, technique: PlateauTechnique) {
        viewModelScope.launch {
            trainingRepository.logPlateauTechnique(
                sessionId = sessionId,
                date = today,
                exerciseName = exerciseName,
                technique = technique,
                notes = null
            )
        }
    }

    fun skipToFinish() = finishSession()

    private fun elapsedSec(): Int = ((MonotonicTimer.now() - startedAtMs) / 1000L).toInt()

    /**
     * Saves the session and, when it stalled, applies the frequency adjustment: one more rest
     * day before the next session. Reached either by running out of exercises or from the
     * stall screen, which has already written the session row.
     */
    fun finishSession(notes: String? = null) {
        viewModelScope.launch {
            val evaluation = ProgressionEngine.evaluate(results)
            trainingRepository.finishSession(
                sessionId = sessionId,
                stalled = evaluation.stalled,
                elapsedSessionTimeSec = elapsedSec(),
                notes = notes
            )
            val restDays = if (evaluation.stalled) {
                trainingRepository.applyStall(today)
            } else {
                trainingRepository.frequency().currentRestDaysBetweenSessions
            }

            // Read the run before closing it, so the number survives the close.
            val run = trainingRepository.currentRun(today)
            val sessionsThisRun = trainingRepository.completedSessionsInCurrentRun()

            // The stall closes the run. applyStall ran first, so the run records the gap the
            // next one will actually train on.
            val completed = if (evaluation.stalled) {
                trainingRepository.endRun(RunEnd.STALL, today)
            } else {
                null
            }

            _stage.value = SessionStage.Summary(
                evaluation = evaluation,
                restDaysNow = restDays,
                elapsedSessionTimeSec = elapsedSec(),
                run = RunProgress(
                    runNumber = run.runNumber,
                    sessionsThisRun = sessionsThisRun,
                    completed = completed
                )
            )
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
