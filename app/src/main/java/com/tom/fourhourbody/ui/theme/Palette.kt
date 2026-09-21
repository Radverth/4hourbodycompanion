package com.tom.fourhourbody.ui.theme

import androidx.compose.ui.graphics.Color
import com.tom.fourhourbody.data.entity.Pillar

/**
 * One accent carries "act now"; the pillar hues only carry identity. Keeping ember
 * exclusive to actions is what lets the eye find the thing to do without reading.
 */
object Palette {
    val Ground = Color(0xFF0E1113)
    val Surface = Color(0xFF12171A)
    val SurfaceRaised = Color(0xFF171C20)
    val Line = Color(0xFF1A2024)
    val LineStrong = Color(0xFF263036)

    val TextPrimary = Color(0xFFF2F4F5)
    val TextSecondary = Color(0xFF8B979E)
    val TextTertiary = Color(0xFF6E7A81)

    /** The only colour that means "do this now". */
    val Ember = Color(0xFFFF6B35)
    val EmberInk = Color(0xFF14100D)
    val EmberSurface = Color(0xFF1C1613)
    val EmberLine = Color(0xFF3D2A1D)
    val EmberText = Color(0xFF9C8877)

    val Gain = Color(0xFFA6D468)
    val Warn = Color(0xFFE5654B)
    val GainSurface = Color(0xFF2B3A22)

    val Training = Ember
    val Stretches = Color(0xFF4ECDC4)
    val Nutrition = Color(0xFF9BC53D)
    val Sleep = Color(0xFF6C8AE4)
    val Cold = Color(0xFF56B4E9)
    val Creatine = Color(0xFFC678DD)

    /** Dimmed dot for a day that was missed, so the chain reads at a glance. */
    val DotEmpty = Color(0xFF2A3437)

    fun of(pillar: Pillar): Color = when (pillar) {
        Pillar.TRAINING -> Training
        Pillar.STRETCHES -> Stretches
        Pillar.NUTRITION -> Nutrition
        Pillar.SLEEP -> Sleep
        Pillar.COLD -> Cold
        Pillar.CREATINE -> Creatine
    }
}
