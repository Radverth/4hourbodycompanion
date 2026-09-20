package com.tom.fourhourbody.ui.creatine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.CreatineLogEntity
import com.tom.fourhourbody.data.repo.CreatineRepository
import com.tom.fourhourbody.data.repo.SettingsRepository
import com.tom.fourhourbody.domain.creatine.CreatineCycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CreatineUiState(
    val date: LocalDate,
    val cycle: CreatineCycle.State,
    val log: CreatineLogEntity?
)

class CreatineViewModel(
    private val creatineRepository: CreatineRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val today = LocalDate.now()

    val state: StateFlow<CreatineUiState> = combine(
        settingsRepository.settings,
        creatineRepository.observe(today)
    ) { settings, log ->
        CreatineUiState(
            date = today,
            cycle = CreatineCycle.stateOn(settings.creatineCycleStartDate, today),
            log = log
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CreatineUiState(today, CreatineCycle.stateOn(null, today), null)
    )

    fun setMorning(taken: Boolean) = update { it.copy(morningTaken = taken) }

    fun setEvening(taken: Boolean) = update { it.copy(eveningTaken = taken) }

    private fun update(transform: (CreatineLogEntity) -> CreatineLogEntity) {
        viewModelScope.launch {
            val start = settingsRepository.current().creatineCycleStartDate
            creatineRepository.update(today, start, transform)
        }
    }

    /** Starting a new cycle resets the counter and drops rows from before the new start. */
    fun startNewCycle(startDate: LocalDate) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(creatineCycleStartDate = startDate) }
            creatineRepository.resetTo(startDate)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CreatineViewModel(container.creatineRepository, container.settingsRepository)
            }
        }
    }
}
