package com.tom.fourhourbody.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "sleep_logs", indices = [Index(value = ["date"], unique = true)])
data class SleepLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The date the night *started* — one row per night. */
    val date: LocalDate,
    /** 67–70°F, or as low as 65°F with [socksUsed]. */
    val roomTempOk: Boolean = false,
    val socksUsed: Boolean = false,
    val darkness: Boolean = false,
    val noScreensBeforeBed: Boolean = false,
    /** Two glasses or fewer AND finished 4+ hours before bed — count and timing together. */
    val wineWithinLimit: Boolean = false,
    val coldExposureBeforeBed: Boolean = false,
    val consistentWakeTime: Boolean = false,
    val qualityRating: Int? = null
)

@Entity(tableName = "cold_exposure_logs", indices = [Index("date")])
data class ColdExposureLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val type: ColdExposureType,
    val durationSec: Int,
    val notes: String? = null
)

@Entity(tableName = "creatine_logs", indices = [Index(value = ["date"], unique = true)])
data class CreatineLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val morningTaken: Boolean = false,
    val eveningTaken: Boolean = false,
    /** 1–28, derived from creatineCycleStartDate in Settings. */
    val cycleDay: Int
)

@Entity(tableName = "measurements", indices = [Index("date")])
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val weightKg: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
    val photoUriFront: String? = null,
    val photoUriSide: String? = null,
    val photoUriBack: String? = null
)
