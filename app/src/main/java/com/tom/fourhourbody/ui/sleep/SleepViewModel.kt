package com.tom.fourhourbody.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.SleepLogEntity
import com.tom.fourhourbody.data.repo.SleepRepository
import com.tom.fourhourbody.domain.SleepNight
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class SleepViewModel(
    private val sleepRepository: SleepRepository
) : ViewModel() {

    val nightDate: LocalDate = SleepNight.currentNightDate(LocalDateTime.now())

    val log: StateFlow<SleepLogEntity?> = sleepRepository.observe(nightDate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Always an edit of the existing night, never a second row for the same date. */
    fun update(transform: (SleepLogEntity) -> SleepLogEntity) {
        viewModelScope.launch { sleepRepository.update(nightDate, transform) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SleepViewModel(container.sleepRepository) }
        }
    }
}
