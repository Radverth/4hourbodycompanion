package com.tom.fourhourbody.ui.stretches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.StretchConfigEntity
import com.tom.fourhourbody.data.entity.StretchLogEntity
import com.tom.fourhourbody.data.repo.StretchRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class StretchConfigViewModel(
    private val stretchRepository: StretchRepository
) : ViewModel() {

    val configs: StateFlow<List<StretchConfigEntity>> = stretchRepository.allConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todaysLogs: StateFlow<List<StretchLogEntity>> = stretchRepository.logsOn(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun update(config: StretchConfigEntity) {
        viewModelScope.launch { stretchRepository.upsertConfig(config) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { StretchConfigViewModel(container.stretchRepository) }
        }
    }
}
