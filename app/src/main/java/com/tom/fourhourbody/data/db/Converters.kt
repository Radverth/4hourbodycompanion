package com.tom.fourhourbody.data.db

import androidx.room.TypeConverter
import com.tom.fourhourbody.data.entity.RunEnd
import com.tom.fourhourbody.data.entity.SessionKind
import com.tom.fourhourbody.data.entity.StickingPointTechnique
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

    @TypeConverter
    fun sessionKindToString(value: SessionKind?): String? = value?.name

    @TypeConverter
    fun stringToSessionKind(value: String?): SessionKind? = value?.let(SessionKind::valueOf)

    @TypeConverter
    fun techniqueToString(value: StickingPointTechnique?): String? = value?.name

    @TypeConverter
    fun stringToTechnique(value: String?): StickingPointTechnique? =
        value?.let(StickingPointTechnique::valueOf)
}
