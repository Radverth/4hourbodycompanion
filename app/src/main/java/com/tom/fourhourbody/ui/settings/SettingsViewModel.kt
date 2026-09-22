package com.tom.fourhourbody.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.SettingsEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val container: AppContainer
) : ViewModel() {

    val settings: StateFlow<SettingsEntity> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsEntity())

    /** Every change re-arms the alarms, so a reminder-time edit takes effect immediately. */
    fun update(transform: (SettingsEntity) -> SettingsEntity) {
        viewModelScope.launch {
            container.settingsRepository.update(transform)
            container.rescheduleReminders()
        }
    }

    fun setIntention(intention: String) =
        update { it.copy(trainingIntention = intention.ifBlank { null }) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container) }
        }
    }
}
