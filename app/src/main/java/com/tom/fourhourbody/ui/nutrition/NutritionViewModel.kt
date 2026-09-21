package com.tom.fourhourbody.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.DamageControlLogEntity
import com.tom.fourhourbody.data.entity.DietDayLogEntity
import com.tom.fourhourbody.data.entity.DietMode
import com.tom.fourhourbody.data.repo.NutritionRepository
import com.tom.fourhourbody.data.repo.SettingsRepository
import com.tom.fourhourbody.data.repo.TrainingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class NutritionUiState(
    val date: LocalDate,
    val day: DietDayLogEntity?,
    val damageControl: DamageControlLogEntity?,
    val defaultMode: DietMode,
    val isTrainingDay: Boolean
) {
    val mode: DietMode get() = day?.mode ?: defaultMode

    /** Every rule held, so the one-tap button has nothing left to do. */
    val allRulesHeld: Boolean
        get() = day?.let { it.avoidedWhiteCarbs && it.noLiquidCalories && it.noFruit } == true
}

class NutritionViewModel(
    private val nutritionRepository: NutritionRepository,
    private val settingsRepository: SettingsRepository,
    trainingRepository: TrainingRepository
) : ViewModel() {

    private val today = LocalDate.now()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dayDetails: Flow<Pair<DietDayLogEntity?, DamageControlLogEntity?>> =
        nutritionRepository.observeDay(today).flatMapLatest { day ->
            if (day == null) {
                flowOf(null to null)
            } else {
                nutritionRepository.observeDamageControl(day.id).map { damage -> day to damage }
            }
        }

    private val trainingDay: Flow<Boolean> = combine(
        trainingRepository.schedule(today),
        trainingRepository.observeSessionsOn(today)
    ) { schedule, sessions -> schedule.dueToday || sessions.any { it.completed } }

    val state: StateFlow<NutritionUiState> = combine(
        dayDetails,
        settingsRepository.settings,
        trainingDay
    ) { (day, damage), settings, isTrainingDay ->
        NutritionUiState(
            date = today,
            day = day,
            damageControl = damage,
            defaultMode = settings.dietMode,
            isTrainingDay = isTrainingDay
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NutritionUiState(today, null, null, DietMode.SLOW_CARB, false)
    )

    val referenceMode: StateFlow<DietMode> = settingsRepository.settings
        .map { it.dietMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DietMode.SLOW_CARB)

    private suspend fun defaultMode(): DietMode = settingsRepository.current().dietMode

    fun setMode(mode: DietMode) {
        viewModelScope.launch {
            nutritionRepository.updateDay(today, defaultMode()) { it.copy(mode = mode) }
        }
    }

    fun setRule(transform: (DietDayLogEntity) -> DietDayLogEntity) {
        viewModelScope.launch {
            nutritionRepository.updateDay(today, defaultMode(), transform)
        }
    }

    /** Swaps which checklist is shown; nothing else about the day is touched. */
    fun setCheatDay(isCheatDay: Boolean) {
        viewModelScope.launch {
            nutritionRepository.setCheatDay(today, defaultMode(), isCheatDay)
        }
    }

    /** One tap for the day that went to plan. */
    fun markDayClean() {
        viewModelScope.launch {
            nutritionRepository.markDayClean(today, defaultMode())
        }
    }

    fun updateDamageControl(transform: (DamageControlLogEntity) -> DamageControlLogEntity) {
        viewModelScope.launch {
            val day = nutritionRepository.ensureDay(today, defaultMode())
            nutritionRepository.updateDamageControl(day.id, transform)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NutritionViewModel(
                    container.nutritionRepository,
                    container.settingsRepository,
                    container.trainingRepository
                )
            }
        }
    }
}
