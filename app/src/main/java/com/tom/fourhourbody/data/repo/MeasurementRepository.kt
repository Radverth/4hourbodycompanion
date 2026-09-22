package com.tom.fourhourbody.data.repo

import com.tom.fourhourbody.data.dao.MeasurementDao
import com.tom.fourhourbody.data.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class MeasurementRepository(private val dao: MeasurementDao) {

    val all: Flow<List<MeasurementEntity>> = dao.observeAll()

    val latest: Flow<MeasurementEntity?> = dao.observeLatest()

    suspend fun upsertFor(date: LocalDate, transform: (MeasurementEntity) -> MeasurementEntity) {
        val existing = dao.getOn(date) ?: MeasurementEntity(date = date)
        dao.upsert(transform(existing))
    }

    suspend fun delete(measurement: MeasurementEntity) = dao.delete(measurement)
}
