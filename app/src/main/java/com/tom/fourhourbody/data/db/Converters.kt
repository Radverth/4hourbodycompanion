package com.tom.fourhourbody.data.db

import androidx.room.TypeConverter
import com.tom.fourhourbody.data.entity.RunEnd
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
    fun runEndToString(value: RunEnd?): String? = value?.name

    @TypeConverter
    fun stringToRunEnd(value: String?): RunEnd? = value?.let(RunEnd::valueOf)
}
