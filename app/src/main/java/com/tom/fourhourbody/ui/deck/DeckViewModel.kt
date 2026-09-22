package com.tom.fourhourbody.ui.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.repo.TrainingRepository
import com.tom.fourhourbody.domain.deck.Deck
import com.tom.fourhourbody.domain.deck.DeckBuilder
import com.tom.fourhourbody.domain.deck.DeckCard
import com.tom.fourhourbody.domain.progress.Attribute
import com.tom.fourhourbody.domain.progress.Attributes
import com.tom.fourhourbody.domain.progress.Stats
import com.tom.fourhourbody.domain.training.SessionScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class DeckViewModel(
    private val trainingRepository: TrainingRepository,
    progressRepository: com.tom.fourhourbody.data.repo.ProgressRepository
) : ViewModel() {

    private val windowDays = 30

    val deck: StateFlow<Deck?> = combine(
        trainingRepository.allConfigs,
        trainingRepository.completedLogs
    ) { configs, logs -> DeckBuilder.build(configs, logs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The character sheet: level is the count of milestones passed, nothing more. */
    val stats: StateFlow<Stats> = progressRepository.stats(LocalDate.now())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats())

    private val adherencePercent: StateFlow<Int> = combine(
        trainingRepository.countCompletedBetween(
            LocalDate.now().minusDays((windowDays - 1).toLong()),
            LocalDate.now()
        ),
        trainingRepository.schedule(LocalDate.now())
    ) { completed, schedule ->
        val expected = SessionScheduler.expectedSessionsIn(windowDays, schedule.restDaysBetween)
        if (expected <= 0) 0 else (completed * 100 / expected).coerceAtMost(100)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val attributes: StateFlow<List<Attribute>> = combine(stats, adherencePercent) { s, percent ->
        Attributes.of(stats = s, adherencePercent = percent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Benches or restores an exercise. Cards are never deleted, only benched — the log behind
     * a benched card stays, and it comes back at the tier it earned.
     */
    fun toggle(card: DeckCard) {
        viewModelScope.launch {
            val id = card.id.removePrefix("exercise:").toLongOrNull() ?: return@launch
            val config = trainingRepository.config(id) ?: return@launch
            trainingRepository.upsertConfig(config.copy(isActive = !card.inDeck))
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { DeckViewModel(container.trainingRepository, container.progressRepository) }
        }
    }
}
