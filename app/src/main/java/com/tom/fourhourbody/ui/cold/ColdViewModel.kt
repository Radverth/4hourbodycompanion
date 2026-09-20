package com.tom.fourhourbody.ui.cold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.ColdExposureLogEntity
import com.tom.fourhourbody.data.entity.ColdExposureType
import com.tom.fourhourbody.data.repo.ColdRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ColdViewModel(
    private val coldRepository: ColdRepository
) : ViewModel() {

    private val today = LocalDate.now()

    val recent: StateFlow<List<ColdExposureLogEntity>> = coldRepository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val countThisWeek: StateFlow<Int> = coldRepository.countBetween(today.minusDays(6), today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun log(type: ColdExposureType, durationSec: Int, notes: String?) {
        viewModelScope.launch { coldRepository.log(today, type, durationSec, notes) }
    }

    fun delete(log: ColdExposureLogEntity) {
        viewModelScope.launch { coldRepository.delete(log) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ColdViewModel(container.coldRepository) }
        }
    }
}
