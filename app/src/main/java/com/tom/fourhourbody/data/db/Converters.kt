package com.tom.fourhourbody.data.db

import androidx.room.TypeConverter
import com.tom.fourhourbody.data.entity.ColdExposureType
import com.tom.fourhourbody.data.entity.DietMode
import com.tom.fourhourbody.data.entity.RunEnd
import com.tom.fourhourbody.data.entity.StretchMode
import com.tom.fourhourbody.data.entity.StretchRoutine
import java.time.DayOfWeek
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun dayOfWeekToInt(value: DayOfWeek?): Int? = value?.value

    @TypeConverter
    fun intToDayOfWeek(value: Int?): DayOfWeek? = value?.let(DayOfWeek::of)

    @TypeConverter
    fun daysToString(value: Set<DayOfWeek>?): String? =
        value?.sortedBy { it.value }?.joinToString(",") { it.value.toString() }

    @TypeConverter
    fun stringToDays(value: String?): Set<DayOfWeek>? = value?.let { raw ->
        if (raw.isBlank()) emptySet()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.map(DayOfWeek::of).toSet()
    }

    @TypeConverter
    fun runEndToString(value: RunEnd?): String? = value?.name

    @TypeConverter
    fun stringToRunEnd(value: String?): RunEnd? = value?.let(RunEnd::valueOf)

    @TypeConverter
    fun dietModeToString(value: DietMode?): String? = value?.name

    @TypeConverter
    fun stringToDietMode(value: String?): DietMode? = value?.let(DietMode::valueOf)

    @TypeConverter
    fun routineToString(value: StretchRoutine?): String? = value?.name

    @TypeConverter
    fun stringToRoutine(value: String?): StretchRoutine? = value?.let(StretchRoutine::valueOf)

    @TypeConverter
    fun stretchModeToString(value: StretchMode?): String? = value?.name

    @TypeConverter
    fun stringToStretchMode(value: String?): StretchMode? = value?.let(StretchMode::valueOf)

    @TypeConverter
    fun coldTypeToString(value: ColdExposureType?): String? = value?.name

    @TypeConverter
    fun stringToColdType(value: String?): ColdExposureType? = value?.let(ColdExposureType::valueOf)
}
