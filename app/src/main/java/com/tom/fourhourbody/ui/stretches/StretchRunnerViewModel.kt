package com.tom.fourhourbody.ui.stretches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.StretchLogEntity
import com.tom.fourhourbody.data.entity.StretchRoutine
import com.tom.fourhourbody.data.repo.StretchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class StretchRunnerViewModel(
    private val stretchRepository: StretchRepository,
    private val routine: StretchRoutine,
    private val weekly: Boolean
) : ViewModel() {

    private val _steps = MutableStateFlow<List<StretchStep>?>(null)
    val steps: StateFlow<List<StretchStep>?> = _steps.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        viewModelScope.launch {
            val configs = stretchRepository.configsForNow(routine).let { all ->
                if (routine == StretchRoutine.DESK_RESET && !weekly) {
                    all.filterNot { it.isWeeklyOnly }
                } else {
                    all
                }
            }
            _steps.value = configs.flatMap { it.toSteps() }
        }
    }

    /** Standalone entry: sessionId stays null, which is what separates it from an inline log. */
    fun save(completions: List<StretchCompletion>) {
        viewModelScope.launch {
            val today = LocalDate.now()
            stretchRepository.logAll(
                completions.map { completion ->
                    StretchLogEntity(
                        sessionId = null,
                        stretchName = completion.name,
                        sideOrPosition = completion.sideOrPosition,
                        durationSec = completion.durationSec,
                        repsCompleted = completion.repsCompleted,
                        routine = completion.routine,
                        date = today
                    )
                }
            )
            _saved.value = true
        }
    }

    companion object {
        fun factory(
            container: AppContainer,
            routine: StretchRoutine,
            weekly: Boolean
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StretchRunnerViewModel(container.stretchRepository, routine, weekly)
            }
        }
    }
}
