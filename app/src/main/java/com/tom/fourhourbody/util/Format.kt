package com.tom.fourhourbody.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val dayFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

fun LocalDate.displayShort(): String = format(dayFormatter)

/** "Monday 21 September" — the quiet dateline at the top of Today. */
private val longDayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())

fun LocalDate.displayLong(): String = format(longDayFormatter)

fun Int.asClock(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%d:%02d".format(minutes, seconds)
}

fun Long.msAsClock(): String = ((this + 999) / 1000).toInt().asClock()

fun Int.asTimeOfDay(): String {
    val hours = this / 60
    val minutes = this % 60
    return "%02d:%02d".format(hours, minutes)
}

fun Double.kgDisplay(): String =
    if (this % 1.0 == 0.0) "${this.roundToInt()} kg" else "%.1f kg".format(this)
