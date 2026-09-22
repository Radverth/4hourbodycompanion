package com.tom.fourhourbody.ui.theme

import androidx.compose.ui.graphics.Color
import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.domain.deck.CardTier

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

    // ---- The character sheet ----------------------------------------------------------
    //
    // A warmer, sootier ground than the rest of the app, brass rules instead of grey lines,
    // and parchment instead of white. Ember stays, as the top of the rarity ladder, so the
    // two halves of the app still belong to each other.

    val Ground2 = Color(0xFF0B0908)
    val PanelDark = Color(0xFF17120D)
    val PanelLit = Color(0xFF1F1811)
    val RuleDark = Color(0xFF3A2E1F)

    val Brass = Color(0xFFC9A227)
    val BrassDim = Color(0xFF7A6520)
    val Parchment = Color(0xFFE6DAC6)
    val ParchmentDim = Color(0xFFA79880)
    val ParchmentFaint = Color(0xFF6E6353)
    val Blood = Color(0xFF8B2119)

    /** The rarity ladder is [com.tom.fourhourbody.domain.deck.CardTier], not a new idea. */
    val RarityUnplayed = Color(0xFF6B6259)
    val RarityOpening = Color(0xFF5B8FB9)
    val RarityEstablished = Brass
    val RarityCornerstone = Ember

    /** Dimmed dot for a day that was missed, so the chain reads at a glance. */
    val DotEmpty = Color(0xFF2A3437)

    fun ofTier(tier: CardTier): Color = when (tier) {
        CardTier.UNPLAYED -> RarityUnplayed
        CardTier.OPENING -> RarityOpening
        CardTier.ESTABLISHED -> RarityEstablished
        CardTier.CORNERSTONE -> RarityCornerstone
    }

    fun of(pillar: Pillar): Color = when (pillar) {
        Pillar.TRAINING -> Training
        Pillar.STRETCHES -> Stretches
        Pillar.NUTRITION -> Nutrition
        Pillar.SLEEP -> Sleep
        Pillar.COLD -> Cold
        Pillar.CREATINE -> Creatine
    }
}
