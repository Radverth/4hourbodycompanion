package com.tom.fourhourbody.domain.shift

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Which half of an alternating two-week rota a given week falls in. */
enum class ShiftWeek { A, B }

/**
 * An alternating two-week office rota — one week early, the next week late.
 *
 * [anchorMonday] is any Monday known to be a week A week; everything else is counted from
 * it, forwards or backwards.
 */
data class ShiftPattern(
    val enabled: Boolean = true,
    val days: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ),
    val anchorMonday: LocalDate,
    val aStartMinutes: Int = 8 * 60,
    val aEndMinutes: Int = 17 * 60,
    val bStartMinutes: Int = 9 * 60,
    val bEndMinutes: Int = 18 * 60
)

/**
 * Where the working day sits, so the app can keep out of it.
 *
 * A prompt only works when it lands at a moment you can actually act — motivation and a
 * reminder are useless without the ability to do the thing. Firing "five minutes resets the
 * hips" at 15:00 on an office day does not just fail, it costs something: it teaches you
 * that this app's notifications are noise, and the next one gets swiped away too.
 *
 * So every reminder is moved out of the shift rather than fired into it.
 */
object ShiftSchedule {

    /** Long enough to be out of the building before the phone asks anything of you. */
    const val AFTER_SHIFT_BUFFER_MINUTES = 15

    fun weekOf(date: LocalDate, anchorMonday: LocalDate): ShiftWeek {
        val weeks = ChronoUnit.WEEKS.between(anchorMonday, mondayOf(date))
        return if (Math.floorMod(weeks, 2L) == 0L) ShiftWeek.A else ShiftWeek.B
    }

    /** Minutes-of-day the shift covers, or null on a day that is not worked. */
    fun windowOn(date: LocalDate, pattern: ShiftPattern): IntRange? {
        if (!pattern.enabled) return null
        if (date.dayOfWeek !in pattern.days) return null
        return when (weekOf(date, pattern.anchorMonday)) {
            ShiftWeek.A -> pattern.aStartMinutes until pattern.aEndMinutes
            ShiftWeek.B -> pattern.bStartMinutes until pattern.bEndMinutes
        }
    }

    fun isWorking(at: LocalDateTime, pattern: ShiftPattern): Boolean {
        val window = windowOn(at.toLocalDate(), pattern) ?: return false
        return (at.hour * 60 + at.minute) in window
    }

    /** End of the shift on [date], or null if the day is free. */
    fun shiftEndOn(date: LocalDate, pattern: ShiftPattern): Int? =
        windowOn(date, pattern)?.last?.plus(1)

    /**
     * Pushes a reminder out of the working day. A time before the shift is left alone —
     * the morning is yours — and one landing inside it moves to just after you finish.
     */
    fun moveOutOfShift(at: LocalDateTime, pattern: ShiftPattern): LocalDateTime {
        if (!isWorking(at, pattern)) return at
        val end = shiftEndOn(at.toLocalDate(), pattern) ?: return at
        val freeAt = end + AFTER_SHIFT_BUFFER_MINUTES
        // A shift ending late enough that the buffer crosses midnight stays on the day.
        val clamped = freeAt.coerceAtMost(23 * 60 + 59)
        return at.toLocalDate().atTime(clamped / 60, clamped % 60)
    }

    /**
     * When the desk-reset prompt should land for someone who cannot stretch at work: once,
     * shortly after the shift ends, framed by how long they have been sitting.
     */
    fun afterShiftOn(date: LocalDate, pattern: ShiftPattern): LocalDateTime? {
        val end = shiftEndOn(date, pattern) ?: return null
        val at = (end + AFTER_SHIFT_BUFFER_MINUTES).coerceAtMost(23 * 60 + 59)
        return date.atTime(at / 60, at % 60)
    }

    /** Hours sat through, for the prompt to say something true rather than generic. */
    fun shiftLengthHours(date: LocalDate, pattern: ShiftPattern): Int? {
        val window = windowOn(date, pattern) ?: return null
        return (window.last + 1 - window.first) / 60
    }

    private fun mondayOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
