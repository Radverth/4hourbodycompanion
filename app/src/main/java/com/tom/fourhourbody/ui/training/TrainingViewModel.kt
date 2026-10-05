package com.tom.fourhourbody.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.FrequencySettingEntity
import com.tom.fourhourbody.data.entity.PlateauTechnique
import com.tom.fourhourbody.data.entity.SessionEntity
import com.tom.fourhourbody.data.repo.RunStatus
import com.tom.fourhourbody.data.repo.TrainingRepository
import com.tom.fourhourbody.data.repo.TrainingSchedule
import com.tom.fourhourbody.domain.run.RunSummary
import com.tom.fourhourbody.domain.training.Plateau
import com.tom.fourhourbody.domain.training.Slots
import com.tom.fourhourbody.domain.training.TrainingConstants
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrainingViewModel(
    private val trainingRepository: TrainingRepository
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

    private val _frequency = MutableStateFlow<FrequencySettingEntity?>(null)
    val frequency: StateFlow<FrequencySettingEntity?> = _frequency.asStateFlow()

    /**
     * The exercise stalled twice running, if any. Read once on open rather than observed: the
     * plateau screen is an offer the user takes or ignores, and a panel that could appear
     * mid-scroll would be the app pushing a tool its source says to use sparingly.
     */
    private val _plateau = MutableStateFlow<Plateau?>(null)
    val plateau: StateFlow<Plateau?> = _plateau.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _frequency.value = trainingRepository.frequency()
            _plateau.value = trainingRepository.plateau()
        }
    }

    fun setCuttingPhase(active: Boolean) {
        viewModelScope.launch {
            trainingRepository.setCuttingPhase(active)
            _frequency.value = trainingRepository.frequency()
        }
    }

    fun logPlateauTechnique(
        exerciseName: String,
        technique: PlateauTechnique
    ) {
        viewModelScope.launch {
            trainingRepository.logPlateauTechnique(
                sessionId = null,
                date = LocalDate.now(),
                exerciseName = exerciseName,
                technique = technique,
                notes = null
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
                    slotName = Slots.PUSH,
                    exerciseName = "New exercise",
                    equipment = "Machine",
                    targetTulMinSec = TrainingConstants.TARGET_TUL_MIN_SEC,
                    targetTulMaxSec = TrainingConstants.TARGET_TUL_MAX_SEC,
                    orderIndex = next
                )
            )
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { TrainingViewModel(container.trainingRepository) }
        }
    }
}
