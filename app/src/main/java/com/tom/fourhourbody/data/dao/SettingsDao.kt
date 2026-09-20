package com.tom.fourhourbody.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tom.fourhourbody.data.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Upsert
    suspend fun upsert(settings: SettingsEntity)

    @Query("SELECT * FROM settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): SettingsEntity?
}
