package com.tom.fourhourbody.data.repo

import com.tom.fourhourbody.data.dao.StretchDao
import com.tom.fourhourbody.data.entity.StretchConfigEntity
import com.tom.fourhourbody.data.entity.StretchLogEntity
import com.tom.fourhourbody.data.entity.StretchRoutine
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * The data side of the single stretch engine. Every entry point — inline from a training
 * session, standalone mobility, standalone desk reset — goes through this, so editing a hold
 * time or rep target in one place changes it everywhere.
 */
class StretchRepository(private val dao: StretchDao) {

    val allConfigs: Flow<List<StretchConfigEntity>> = dao.observeAllConfigs()

    suspend fun configsForNow(routine: StretchRoutine): List<StretchConfigEntity> =
        dao.getConfigsFor(routine)

    suspend fun upsertConfig(config: StretchConfigEntity) = dao.upsertConfig(config)

    /** [sessionId] is set for inline entries and left null for standalone ones. */
    suspend fun logAll(logs: List<StretchLogEntity>) = dao.insertLogs(logs)

    fun logsOn(date: LocalDate): Flow<List<StretchLogEntity>> = dao.observeLogsOn(date)

    fun logsBetween(from: LocalDate, to: LocalDate): Flow<List<StretchLogEntity>> =
        dao.observeLogsBetween(from, to)

    fun countStandaloneBetween(
        routine: StretchRoutine,
        from: LocalDate,
        to: LocalDate
    ): Flow<Int> = dao.countStandaloneBetween(routine, from, to)

}
