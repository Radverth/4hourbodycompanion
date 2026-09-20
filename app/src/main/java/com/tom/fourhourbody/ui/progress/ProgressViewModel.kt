package com.tom.fourhourbody.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tom.fourhourbody.AppContainer
import com.tom.fourhourbody.data.entity.MeasurementEntity
import com.tom.fourhourbody.data.repo.MeasurementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class PhotoSlot { FRONT, SIDE, BACK }

class ProgressViewModel(
    private val measurementRepository: MeasurementRepository
) : ViewModel() {

    private val today = LocalDate.now()

    val measurements: StateFlow<List<MeasurementEntity>> = measurementRepository.all
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latest: StateFlow<MeasurementEntity?> = measurementRepository.latest
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveMeasurement(weightKg: Double?, waistCm: Double?, hipCm: Double?) {
        viewModelScope.launch {
            measurementRepository.upsertFor(today) {
                it.copy(weightKg = weightKg, waistCm = waistCm, hipCm = hipCm)
            }
        }
    }

    fun setPhoto(slot: PhotoSlot, uri: String) {
        viewModelScope.launch {
            measurementRepository.upsertFor(today) {
                when (slot) {
                    PhotoSlot.FRONT -> it.copy(photoUriFront = uri)
                    PhotoSlot.SIDE -> it.copy(photoUriSide = uri)
                    PhotoSlot.BACK -> it.copy(photoUriBack = uri)
                }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ProgressViewModel(container.measurementRepository) }
        }
    }
}
