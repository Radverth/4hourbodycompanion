package com.tom.fourhourbody.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.repo.DashboardRepository
import com.tom.fourhourbody.data.repo.DashboardState
import com.tom.fourhourbody.data.repo.ProgressRepository
import com.tom.fourhourbody.data.repo.RunStatus
import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.data.repo.SettingsRepository
import com.tom.fourhourbody.data.repo.TrainingRepository
import com.tom.fourhourbody.domain.progress.Milestones
import com.tom.fourhourbody.domain.today.Focus
import com.tom.fourhourbody.domain.today.FocusInputs
import com.tom.fourhourbody.domain.today.FocusRules
import com.tom.fourhourbody.domain.training.SessionPlanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** What the hero card promises you for turning up. */
data class NextWeight(val exerciseName: String, val weightKg: Double, val gainKg: Double)

class DashboardViewModel(
    dashboardRepository: DashboardRepository,
    private val trainingRepository: TrainingRepository,
    progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val today = LocalDate.now()

    val state: StateFlow<DashboardState?> = dashboardRepository.today(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings: StateFlow<SettingsEntity> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsEntity())

    private val _nextWeights = MutableStateFlow<List<NextWeight>>(emptyList())
    val nextWeights: StateFlow<List<NextWeight>> = _nextWeights.asStateFlow()

    /** Level, for the standing line. The deck is where it is broken down. */
    val level: StateFlow<Int> = progressRepository.stats(today)
        .map(Milestones::level)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Always present, so the header never has to render nothing. */
    val runStatus: StateFlow<RunStatus?> = trainingRepository.runStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** What Today is about — there is only one pillar, so this is a plain due/not-due. */
    val focus: StateFlow<Focus> = state.map { current ->
        if (current == null) {
            Focus.CLEAR
        } else {
            FocusRules.pick(
                FocusInputs(
                    sessionDueToday = current.training.dueToday,
                    sessionCompletedToday = current.training.completedToday
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Focus.CLEAR)

    init {
        viewModelScope.launch { loadNextWeights() }
    }

    /**
     * The reward for last session, surfaced before this one starts. The progression rule
     * already computes it; the app simply never showed it.
     *
     * Asks the planner for the session that would actually run rather than for every
     * configured slot, so the cutting phase and the Big Three toggle are reflected here and
     * the board rows — which only exist for a no-equipment night — are not listed as weights
     * waiting on a machine.
     */
    private suspend fun loadNextWeights() {
        val frequency = trainingRepository.frequency()
        val settings = settingsRepository.current()
        val kind = SessionPlanner.kindFor(frequency.cuttingPhaseActive, hasEquipment = true)
        _nextWeights.value = trainingRepository
            .exercisesFor(kind, settings.bigThreeOnly)
            .mapNotNull { config ->
                val last = trainingRepository.lastLogFor(config.exerciseName)
                    ?: return@mapNotNull null
                val next = trainingRepository.openingWeightFor(config) ?: return@mapNotNull null
                // Nothing to announce when the weight is unchanged: holding a weight is the
                // normal case, and a row saying "same as last time" is not a reward.
                if (next <= last.weightKg + 0.01) return@mapNotNull null
                NextWeight(
                    exerciseName = config.exerciseName,
                    weightKg = next,
                    gainKg = next - last.weightKg
                )
            }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    container.dashboardRepository,
                    container.trainingRepository,
                    container.progressRepository,
                    container.settingsRepository
                )
            }
        }
    }
}
