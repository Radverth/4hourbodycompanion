package com.tom.fourhourbody.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.ExerciseConfigEntity
import com.tom.fourhourbody.data.entity.SessionEntity
import com.tom.fourhourbody.data.repo.RunStatus
import com.tom.fourhourbody.data.repo.TrainingRepository
import com.tom.fourhourbody.data.repo.TrainingSchedule
import com.tom.fourhourbody.domain.run.RunSummary
import com.tom.fourhourbody.domain.training.TrainingConstants
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

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
                    targetReps = TrainingConstants.DEFAULT_TARGET_REPS,
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
