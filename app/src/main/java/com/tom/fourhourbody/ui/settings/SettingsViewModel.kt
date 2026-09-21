package com.tom.fourhourbody.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.data.entity.SettingsEntity
import com.tom.fourhourbody.domain.shift.ShiftSchedule
import com.tom.fourhourbody.domain.shift.ShiftWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val container: AppContainer
) : ViewModel() {

    val settings: StateFlow<SettingsEntity> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsEntity())

    /**
     * Every change re-arms the alarms, so switching a pillar off stops its reminders
     * immediately — and switching it back on restores them — without touching its history.
     */
    fun update(transform: (SettingsEntity) -> SettingsEntity) {
        viewModelScope.launch {
            container.settingsRepository.update(transform)
            container.rescheduleReminders()
        }
    }

    fun setPillar(pillar: Pillar, enabled: Boolean) = update { it.withPillar(pillar, enabled) }

    fun setIntention(pillar: Pillar, intention: String) =
        update { it.withIntention(pillar, intention.ifBlank { null }) }

    /**
     * Anchors the rota from the week you are in now, which is the only question anyone can
     * answer without a calendar: is this week the early one or the late one?
     */
    fun anchorThisWeek(isEarlyWeek: Boolean) {
        val thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        update {
            it.copy(
                shiftEnabled = true,
                shiftAnchorMonday = if (isEarlyWeek) thisMonday else thisMonday.minusWeeks(1)
            )
        }
    }

    /** Which half of the rota today falls in, for the settings screen to show back. */
    fun currentWeek(settings: SettingsEntity): ShiftWeek? =
        settings.shiftAnchorMonday?.let { ShiftSchedule.weekOf(LocalDate.now(), it) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container) }
        }
    }
}
