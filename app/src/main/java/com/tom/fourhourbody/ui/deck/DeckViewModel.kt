package com.tom.fourhourbody.ui.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.repo.DashboardRepository
import com.tom.fourhourbody.data.repo.ProgressRepository
import com.tom.fourhourbody.data.repo.SettingsRepository
import com.tom.fourhourbody.data.repo.TrainingRepository
import com.tom.fourhourbody.domain.deck.CardKind
import com.tom.fourhourbody.domain.deck.Deck
import com.tom.fourhourbody.domain.deck.DeckBuilder
import com.tom.fourhourbody.domain.adherence.PillarAdherence
import com.tom.fourhourbody.domain.deck.DeckCard
import com.tom.fourhourbody.domain.progress.Stats
import com.tom.fourhourbody.domain.synergy.SynergyState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class DeckViewModel(
    private val settingsRepository: SettingsRepository,
    private val trainingRepository: TrainingRepository,
    dashboardRepository: DashboardRepository,
    progressRepository: ProgressRepository
) : ViewModel() {

    /**
     * A pillar's tier reads the last 30 days, so it reflects what is being run now rather
     * than what was run once. See [DeckBuilder].
     */
    private val windowDays = 30

    val deck: StateFlow<Deck?> = combine(
        dashboardRepository.adherenceAll(LocalDate.now(), windowDays),
        trainingRepository.allConfigs,
        trainingRepository.completedLogs
    ) { (settings, adherence), configs, logs ->
        DeckBuilder.build(
            adherence = adherence,
            isEnabled = settings::isEnabled,
            configs = configs,
            logs = logs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The character sheet: level is the count of milestones passed, nothing more. */
    val stats: StateFlow<Stats> = progressRepository.stats(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats())

    /**
     * Adherence, which used to sit under Today. It is review, not action, so it belongs on
     * the screen you open when you want to look rather than do.
     */
    val adherence: StateFlow<List<PillarAdherence>> =
        dashboardRepository.adherence(LocalDate.now(), windowDays)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The book's own pairings, over the same window the pillar tiers read. */
    val synergies: StateFlow<List<SynergyState>> =
        dashboardRepository.synergies(LocalDate.now(), windowDays)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Moves a card in or out of the deck. Cards are never deleted, only benched — the log
     * behind a benched card stays, and it comes back at the tier it earned.
     */
    fun toggle(card: DeckCard) {
        viewModelScope.launch {
            when (card.kind) {
                CardKind.PILLAR -> settingsRepository.setPillarEnabled(card.pillar, !card.inDeck)
                CardKind.EXERCISE -> {
                    val id = card.id.removePrefix("exercise:").toLongOrNull() ?: return@launch
                    val config = trainingRepository.config(id) ?: return@launch
                    trainingRepository.upsertConfig(config.copy(isActive = !card.inDeck))
                }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DeckViewModel(
                    container.settingsRepository,
                    container.trainingRepository,
                    container.dashboardRepository,
                    container.progressRepository
                )
            }
        }
    }
}
