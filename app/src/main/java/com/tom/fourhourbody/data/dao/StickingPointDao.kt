package com.tom.fourhourbody.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tom.fourhourbody.data.entity.StickingPointLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StickingPointDao {

    @Insert
    suspend fun insert(log: StickingPointLogEntity): Long

    @Query("SELECT * FROM sticking_point_logs ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<StickingPointLogEntity>>

    @Query(
        """
        SELECT * FROM sticking_point_logs
        WHERE exerciseName = :exerciseName ORDER BY date DESC, id DESC
        """
    )
    fun observeFor(exerciseName: String): Flow<List<StickingPointLogEntity>>
}
