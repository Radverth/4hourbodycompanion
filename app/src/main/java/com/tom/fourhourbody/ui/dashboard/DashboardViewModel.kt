package com.tom.fourhourbody.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.repo.DashboardRepository
import com.tom.fourhourbody.data.repo.DashboardState
import com.tom.fourhourbody.domain.SleepNight
import com.tom.fourhourbody.domain.adherence.PillarAdherence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime

class DashboardViewModel(
    dashboardRepository: DashboardRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val nightDate = SleepNight.currentNightDate(LocalDateTime.now())

    private val _window = MutableStateFlow(7)
    val window: StateFlow<Int> = _window.asStateFlow()

    val state: StateFlow<DashboardState?> = dashboardRepository.today(today, nightDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val adherence: StateFlow<List<PillarAdherence>> = _window
        .flatMapLatest { days -> dashboardRepository.adherence(today, days) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setWindow(days: Int) {
        _window.value = days
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { DashboardViewModel(container.dashboardRepository) }
        }
    }
}
