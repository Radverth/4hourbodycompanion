package com.tom.fourhourbody.data.repo

import com.tom.fourhourbody.data.dao.SettingsDao
import com.tom.fourhourbody.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dao: SettingsDao) {

    /** Never null downstream — the seeder writes row 1, and defaults stand in until it does. */
    val settings: Flow<SettingsEntity> = dao.observe().map { it ?: SettingsEntity() }

    suspend fun current(): SettingsEntity = dao.get() ?: SettingsEntity()

    suspend fun update(transform: (SettingsEntity) -> SettingsEntity) {
        dao.upsert(transform(current()))
    }
}
