package com.tom.fourhourbody.data.repo

import com.tom.fourhourbody.data.dao.ColdDao
import com.tom.fourhourbody.data.dao.CreatineDao
import com.tom.fourhourbody.data.dao.MeasurementDao
import com.tom.fourhourbody.data.dao.SleepDao
import com.tom.fourhourbody.data.entity.ColdExposureLogEntity
import com.tom.fourhourbody.data.entity.ColdExposureType
import com.tom.fourhourbody.data.entity.CreatineLogEntity
import com.tom.fourhourbody.data.entity.MeasurementEntity
import com.tom.fourhourbody.data.entity.SleepLogEntity
import com.tom.fourhourbody.domain.creatine.CreatineCycle
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class SleepRepository(private val dao: SleepDao) {

    fun observe(date: LocalDate): Flow<SleepLogEntity?> = dao.observe(date)

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<SleepLogEntity>> =
        dao.observeBetween(from, to)

    suspend fun get(date: LocalDate): SleepLogEntity? = dao.get(date)

    /** Edits the existing night rather than adding a second row for the same date. */
    suspend fun update(date: LocalDate, transform: (SleepLogEntity) -> SleepLogEntity) {
        val existing = dao.get(date) ?: SleepLogEntity(date = date)
        dao.upsert(transform(existing))
    }
}

class ColdRepository(private val dao: ColdDao) {

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<ColdExposureLogEntity>> =
        dao.observeBetween(from, to)

    fun countBetween(from: LocalDate, to: LocalDate): Flow<Int> = dao.countBetween(from, to)

    fun observeRecent(limit: Int = 20): Flow<List<ColdExposureLogEntity>> = dao.observeRecent(limit)

    suspend fun log(date: LocalDate, type: ColdExposureType, durationSec: Int, notes: String?) =
        dao.insert(
            ColdExposureLogEntity(date = date, type = type, durationSec = durationSec, notes = notes)
        )

    suspend fun delete(log: ColdExposureLogEntity) = dao.delete(log)
}

class CreatineRepository(private val dao: CreatineDao) {

    fun observe(date: LocalDate): Flow<CreatineLogEntity?> = dao.observe(date)

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<CreatineLogEntity>> =
        dao.observeBetween(from, to)

    /**
     * Writes are only meaningful while a cycle is running — the cycle day comes from the start
     * date in Settings, never from a counter that could drift.
     */
    suspend fun update(
        date: LocalDate,
        cycleStartDate: LocalDate?,
        transform: (CreatineLogEntity) -> CreatineLogEntity
    ) {
        val day = CreatineCycle.stateOn(cycleStartDate, date).day ?: return
        val existing = dao.get(date) ?: CreatineLogEntity(date = date, cycleDay = day)
        dao.upsert(transform(existing.copy(cycleDay = day)))
    }

    /** Starting a fresh cycle drops rows from before the new start date. */
    suspend fun resetTo(startDate: LocalDate) = dao.deleteBefore(startDate)
}

class MeasurementRepository(private val dao: MeasurementDao) {

    val all: Flow<List<MeasurementEntity>> = dao.observeAll()

    val latest: Flow<MeasurementEntity?> = dao.observeLatest()

    suspend fun upsertFor(date: LocalDate, transform: (MeasurementEntity) -> MeasurementEntity) {
        val existing = dao.getOn(date) ?: MeasurementEntity(date = date)
        dao.upsert(transform(existing))
    }

    suspend fun delete(measurement: MeasurementEntity) = dao.delete(measurement)
}
