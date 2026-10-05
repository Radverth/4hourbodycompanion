package com.tom.fourhourbody.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.data.repo.CreatineRepository
import com.tom.fourhourbody.data.repo.DashboardRepository
import com.tom.fourhourbody.data.repo.DashboardState
import com.tom.fourhourbody.data.repo.MotivationRepository
import com.tom.fourhourbody.data.repo.MotivationState
import com.tom.fourhourbody.data.repo.NutritionRepository
import com.tom.fourhourbody.data.repo.ProgressRepository
import com.tom.fourhourbody.data.repo.SettingsRepository
import com.tom.fourhourbody.data.repo.RunStatus
import com.tom.fourhourbody.data.repo.TrainingRepository
import com.tom.fourhourbody.domain.SleepNight
import com.tom.fourhourbody.domain.progress.Milestones
import com.tom.fourhourbody.domain.today.Focus
import com.tom.fourhourbody.domain.today.FocusInputs
import com.tom.fourhourbody.domain.today.FocusRules
import com.tom.fourhourbody.domain.training.SessionPlanner
import com.tom.fourhourbody.util.kgDisplay
import com.tom.fourhourbody.domain.synergy.SynergyEngine
import com.tom.fourhourbody.domain.synergy.SynergyState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** What the hero card promises you for turning up. */
/**
 * What last session earned for this one. [position] carries the board handle position for
 * bodyweight work, where the load cannot move in kilos.
 */
data class NextWeight(
    val exerciseName: String,
    val weightKg: Double,
    val gainKg: Double,
    val position: String? = null
) {
    /** Kilos when there are plates to add, the handle position when there are not. */
    fun loadDisplay(): String = if (weightKg > 0.01) weightKg.kgDisplay() else position ?: "—"
}

class DashboardViewModel(
    dashboardRepository: DashboardRepository,
    motivationRepository: MotivationRepository,
    private val trainingRepository: TrainingRepository,
    private val nutritionRepository: NutritionRepository,
    private val creatineRepository: CreatineRepository,
    progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val nightDate = SleepNight.currentNightDate(LocalDateTime.now())

    val state: StateFlow<DashboardState?> = dashboardRepository.today(today, nightDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val motivation: StateFlow<MotivationState?> = motivationRepository.state(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _nextWeights = MutableStateFlow<List<NextWeight>>(emptyList())
    val nextWeights: StateFlow<List<NextWeight>> = _nextWeights.asStateFlow()

    /** Level, for the standing line. The deck is where it is broken down. */
    val level: StateFlow<Int> = progressRepository.stats(today)
        .map(Milestones::level)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Always present, so the header never has to render nothing. */
    val runStatus: StateFlow<RunStatus?> = trainingRepository.runStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The one pairing with a half still open today, if there is one. Deliberately not tied to
     * the adherence window selector — this is about today, not about a rate.
     */
    val synergyNudge: StateFlow<SynergyState?> =
        dashboardRepository.synergies(today, NUDGE_WINDOW_DAYS)
            .map(SynergyEngine::liveNudge)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * What Today is about. Everything else on the screen is a mark, not a card, so this is
     * the only thing competing for attention.
     */
    val focus: StateFlow<Focus> = combine(state, synergyNudge) { current, nudge ->
        if (current == null) {
            Focus.CLEAR
        } else {
            FocusRules.pick(
                FocusInputs(
                    trainingEnabled = current.settings.isEnabled(Pillar.TRAINING),
                    sessionDueToday = current.training.dueToday,
                    sessionCompletedToday = current.training.completedToday,
                    comboHalfOpen = nudge != null,
                    nutritionEnabled = current.settings.isEnabled(Pillar.NUTRITION),
                    dayLogged = current.nutrition.dayLogged
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
     */
    private suspend fun loadNextWeights() {
        val frequency = trainingRepository.frequency()
        val settings = settingsRepository.current()
        val kind = SessionPlanner.kindFor(frequency.cuttingPhaseActive, hasEquipment = true)
        _nextWeights.value = trainingRepository
            .exercisesFor(kind, settings.bigThreeOnly)
            .mapNotNull { config ->
                val last = trainingRepository.lastSetFor(config.exerciseName)
                    ?: return@mapNotNull null
                val next = trainingRepository.openingLoadFor(config) ?: return@mapNotNull null
                // Nothing to announce when the load is unchanged — staying inside the window
                // is the normal case, and a row saying "same as last time" is not a reward.
                if (next.sameAs(last.load)) return@mapNotNull null
                NextWeight(
                    exerciseName = config.exerciseName,
                    weightKg = next.weightKg,
                    gainKg = next.weightKg - last.load.weightKg,
                    position = next.position
                )
            }
    }

    /** One tap for a day that went to plan, without leaving the dashboard. */
    fun markDayClean() {
        viewModelScope.launch {
            nutritionRepository.markDayClean(today, settingsRepository.current().dietMode)
        }
    }

    /**
     * The next dose, in one tap. Creatine is a yes/no per half-day, so making it cost a
     * screen was pure friction.
     */
    fun takeNextCreatineDose() {
        viewModelScope.launch {
            val start = settingsRepository.current().creatineCycleStartDate
            creatineRepository.update(today, start) { log ->
                if (!log.morningTaken) log.copy(morningTaken = true) else log.copy(eveningTaken = true)
            }
        }
    }

    companion object {
        private const val NUDGE_WINDOW_DAYS = 7

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    container.dashboardRepository,
                    container.motivationRepository,
                    container.trainingRepository,
                    container.nutritionRepository,
                    container.creatineRepository,
                    container.progressRepository,
                    container.settingsRepository
                )
            }
        }
    }
}
