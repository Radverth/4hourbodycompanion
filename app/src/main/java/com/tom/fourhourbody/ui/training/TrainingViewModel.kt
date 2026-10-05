package com.tom.fourhourbody.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.FrequencySettingEntity
import com.tom.fourhourbody.data.entity.SessionEntity
import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.data.entity.StickingPointTechnique
import com.tom.fourhourbody.data.repo.RunStatus
import com.tom.fourhourbody.data.repo.SettingsRepository
import com.tom.fourhourbody.data.repo.TrainingRepository
import com.tom.fourhourbody.data.repo.TrainingSchedule
import com.tom.fourhourbody.domain.run.RunSummary
import com.tom.fourhourbody.domain.training.StickingPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TrainingViewModel(
    private val trainingRepository: TrainingRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val schedule: StateFlow<TrainingSchedule?> = trainingRepository.schedule(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sessions: StateFlow<List<SessionEntity>> = trainingRepository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every run, newest first. The first entry is the open one whenever a run is running. */
    val runs: StateFlow<List<RunSummary>> = trainingRepository.runHistory(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Always present, so the run panel renders before the first session as well as after. */
    val runStatus: StateFlow<RunStatus?> = trainingRepository.runStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val configs: StateFlow<List<ExerciseConfigEntity>> = trainingRepository.allConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val frequency: StateFlow<FrequencySettingEntity?> = trainingRepository.observeFrequency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings: StateFlow<SettingsEntity?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The exercise stuck twice running, if any. Read once on open rather than observed: the
     * techniques are an offer the user takes or ignores, and a panel that appeared mid-scroll
     * would be the app pushing a tool its own source says to use sparingly.
     */
    private val _stickingPoint = MutableStateFlow<StickingPoint?>(null)
    val stickingPoint: StateFlow<StickingPoint?> = _stickingPoint.asStateFlow()

    init {
        viewModelScope.launch { _stickingPoint.value = trainingRepository.stickingPoint() }
    }

    fun setCuttingPhase(active: Boolean) {
        viewModelScope.launch { trainingRepository.setCuttingPhase(active) }
    }

    fun setBigThreeOnly(active: Boolean) {
        viewModelScope.launch { settingsRepository.update { it.copy(bigThreeOnly = active) } }
    }

    fun logStickingPointTechnique(exerciseName: String, technique: StickingPointTechnique) {
        viewModelScope.launch {
            trainingRepository.logStickingPointTechnique(
                sessionId = null,
                date = LocalDate.now(),
                exerciseName = exerciseName,
                technique = technique
            )
        }
    }

    fun updateConfig(config: ExerciseConfigEntity) {
        viewModelScope.launch { trainingRepository.upsertConfig(config) }
    }

    fun addConfig() {
        viewModelScope.launch {
            val next = (configs.value.maxOfOrNull { it.orderIndex } ?: -1) + 1
            trainingRepository.upsertConfig(
                ExerciseConfigEntity(
                    slotName = "Slot ${next + 1}",
                    exerciseName = "New exercise",
                    equipment = "Machine",
                    orderIndex = next
                )
            )
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TrainingViewModel(container.trainingRepository, container.settingsRepository)
            }
        }
    }
}
