package com.tom.fourhourbody.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.ExerciseLogEntity
import com.tom.fourhourbody.data.entity.RunEnd
import com.tom.fourhourbody.data.entity.SessionKind
import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.data.entity.StickingPointTechnique
import com.tom.fourhourbody.data.repo.SettingsRepository
import com.tom.fourhourbody.data.repo.TrainingRepository
import com.tom.fourhourbody.domain.run.RunSummary
import com.tom.fourhourbody.domain.training.ExerciseResult
import com.tom.fourhourbody.domain.training.ProgressionEngine
import com.tom.fourhourbody.domain.training.SessionEvaluation
import com.tom.fourhourbody.domain.training.StickingPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Where the guided session currently is. */
sealed interface SessionStage {
    data object Loading : SessionStage

    /** One-time, dismissible cue before the first set. */
    data object FirstSetCue : SessionStage

    data class Strength(val index: Int) : SessionStage
    data class Rest(val nextIndex: Int) : SessionStage

    data class Summary(
        val evaluation: SessionEvaluation,
        val restDaysNow: Int,
        val run: RunProgress
    ) : SessionStage
}

/**
 * Where this session sat in its run, and — when the session's plateau closed the run — what
 * the whole run earned.
 */
data class RunProgress(
    val runNumber: Int,
    val sessionsThisRun: Int,
    val completed: RunSummary?
)

data class ExercisePrompt(
    val config: ExerciseConfigEntity,
    val suggestedWeightKg: Double?,
    val suggestedPosition: String?,
    val lastWeightKg: Double?,
    val lastTulSec: Int?,
    val lastPosition: String?,
    /** Heaviest ever logged for this exercise — what a set has to beat to be a record. */
    val bestEverKg: Double?,
    val position: Int,
    val total: Int
) {
    val isBodyweight: Boolean get() = config.isBodyweight

    /** A record is called while the weight is still on the bar, not in the summary. */
    fun isRecord(weightKg: Double): Boolean =
        bestEverKg != null && weightKg > bestEverKg + 0.01
}

class SessionViewModel(
    private val trainingRepository: TrainingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _stage = MutableStateFlow<SessionStage>(SessionStage.Loading)
    val stage: StateFlow<SessionStage> = _stage.asStateFlow()

    private val _prompt = MutableStateFlow<ExercisePrompt?>(null)
    val prompt: StateFlow<ExercisePrompt?> = _prompt.asStateFlow()

    private val _settings = MutableStateFlow(SettingsEntity())
    val settings: StateFlow<SettingsEntity> = _settings.asStateFlow()

    private val _kind = MutableStateFlow(SessionKind.STANDARD)
    val kind: StateFlow<SessionKind> = _kind.asStateFlow()

    /**
     * The exercise stuck twice running, read once when the session is saved. Shown on the
     * summary rather than offered mid-session: the techniques apply to the *next* attempt,
     * and interrupting a session to suggest one would be the app pushing a tool its own
     * source says to use sparingly.
     */
    private val _stickingPoint = MutableStateFlow<StickingPoint?>(null)
    val stickingPoint: StateFlow<StickingPoint?> = _stickingPoint.asStateFlow()

    private var sessionId: Long = 0
    private var configs: List<ExerciseConfigEntity> = emptyList()
    private val results = mutableListOf<ExerciseResult>()
    private val today = LocalDate.now()

    /** Rest actually taken before the next exercise, carried into its log row. */
    private var pendingRestSec: Int = 0

    fun start(requestedKind: SessionKind) {
        viewModelScope.launch {
            val loaded = settingsRepository.current()
            _settings.value = loaded
            val frequency = trainingRepository.frequency()
            // A standard request becomes a cutting session when the phase is on; an explicit
            // no-equipment request stays what it asked for.
            val kind = if (requestedKind == SessionKind.STANDARD && frequency.cuttingPhaseActive) {
                SessionKind.CUTTING
            } else {
                requestedKind
            }
            _kind.value = kind
            configs = trainingRepository.exercisesFor(kind, loaded.bigThreeOnly)

            sessionId = trainingRepository.startSession(today, kind)

            if (loaded.firstSetCueDismissed) {
                if (configs.isEmpty()) {
                    finishSession()
                } else {
                    loadPrompt(0)
                    _stage.value = SessionStage.Strength(0)
                }
            } else {
                _stage.value = SessionStage.FirstSetCue
            }
        }
    }

    fun acknowledgeFirstSetCue(dontShowAgain: Boolean) {
        viewModelScope.launch {
            if (dontShowAgain) {
                settingsRepository.update { it.copy(firstSetCueDismissed = true) }
            }
            if (configs.isEmpty()) {
                finishSession()
            } else {
                loadPrompt(0)
                _stage.value = SessionStage.Strength(0)
            }
        }
    }

    private suspend fun loadPrompt(index: Int) {
        val config = configs.getOrNull(index) ?: return
        val last = trainingRepository.lastLogFor(config.exerciseName)
        _prompt.value = ExercisePrompt(
            config = config,
            suggestedWeightKg = trainingRepository.openingWeightFor(config),
            suggestedPosition = trainingRepository.openingPositionFor(config),
            lastWeightKg = last?.weightKg,
            lastTulSec = last?.tulSec,
            lastPosition = last?.position,
            bestEverKg = trainingRepository.bestEverFor(config.exerciseName),
            position = index + 1,
            total = configs.size
        )
    }

    /**
     * Log one exercise. Nothing in the book supports stopping a session over one result, so
     * every exercise always runs — whether the session plateaued is decided once, at the end.
     */
    fun logExercise(index: Int, weightKg: Double, position: String?, tulSec: Int) {
        val config = configs.getOrNull(index) ?: return
        val previous = _prompt.value
        viewModelScope.launch {
            val cleaned = position?.trim()?.takeIf { it.isNotEmpty() }
            trainingRepository.logExercise(
                ExerciseLogEntity(
                    sessionId = sessionId,
                    exerciseName = config.exerciseName,
                    equipment = config.equipment,
                    weightKg = weightKg,
                    position = cleaned,
                    tulSec = tulSec,
                    restSecActual = pendingRestSec
                )
            )
            results += ExerciseResult(
                exerciseName = config.exerciseName,
                weightKg = weightKg,
                tulSec = tulSec,
                previousWeightKg = previous?.lastWeightKg,
                previousTulSec = previous?.lastTulSec,
                position = cleaned,
                previousPosition = previous?.lastPosition
            )
            pendingRestSec = 0

            if (index == configs.lastIndex) {
                finishSession()
            } else {
                _stage.value = SessionStage.Rest(index + 1)
            }
        }
    }

    fun onRestFinished(actualRestSec: Int, nextIndex: Int) {
        pendingRestSec = actualRestSec
        viewModelScope.launch {
            loadPrompt(nextIndex)
            _stage.value = SessionStage.Strength(nextIndex)
        }
    }

    fun skipToFinish() = finishSession()

    fun logStickingPointTechnique(exerciseName: String, technique: StickingPointTechnique) {
        viewModelScope.launch {
            trainingRepository.logStickingPointTechnique(
                sessionId = sessionId,
                date = today,
                exerciseName = exerciseName,
                technique = technique
            )
        }
    }

    /**
     * Saves the session and, when it plateaued, applies the book's frequency adjustment: one
     * more rest day before the next session.
     */
    fun finishSession(notes: String? = null) {
        viewModelScope.launch {
            val evaluation = ProgressionEngine.evaluate(results)
            trainingRepository.finishSession(
                sessionId,
                evaluation.plateaued,
                evaluation.plateauedOn,
                notes
            )
            val restDays = if (evaluation.plateaued) {
                trainingRepository.applyPlateau(today)
            } else {
                trainingRepository.frequency().currentRestDaysBetweenSessions
            }

            // Read the run before closing it, so the number survives the close.
            val run = trainingRepository.currentRun(today)
            val sessionsThisRun = trainingRepository.completedSessionsInCurrentRun()

            // The plateau closes the run. applyPlateau ran first, so the run records the gap
            // the next one will actually train on.
            val completed = if (evaluation.plateaued) {
                trainingRepository.endRun(RunEnd.PLATEAU, today)
            } else {
                null
            }

            // Read after the session row is saved, so this session's plateau counts toward it.
            _stickingPoint.value = trainingRepository.stickingPoint()

            _stage.value = SessionStage.Summary(
                evaluation = evaluation,
                restDaysNow = restDays,
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
                    container.settingsRepository
                )
            }
        }
    }
}
