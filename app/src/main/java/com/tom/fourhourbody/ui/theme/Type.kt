package com.tom.fourhourbody.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tom.fourhourbody.R

/** Archivo for headings and numerals; IBM Plex Sans for everything read as prose. */
val Archivo = FontFamily(
    Font(R.font.archivo_bold, FontWeight.Bold),
    Font(R.font.archivo_extrabold, FontWeight.ExtraBold)
)

/**
 * The character sheet's face. Cinzel is a Roman inscriptional letterform — the convention
 * action RPGs reach for, and deliberately unlike Archivo so the two never read as one voice.
 * Caps only, and sparingly: it is a display face, not a reading face.
 */
val Cinzel = FontFamily(
    Font(R.font.cinzel_semibold, FontWeight.SemiBold),
    Font(R.font.cinzel_bold, FontWeight.Bold)
)

val PlexSans = FontFamily(
    Font(R.font.plex_sans_regular, FontWeight.Normal),
    Font(R.font.plex_sans_medium, FontWeight.Medium),
    Font(R.font.plex_sans_semibold, FontWeight.SemiBold)
)

/**
 * Numbers are the motivating content in this app — weights, reps, streaks, percentages —
 * so they get the display face at sizes meant to be read across a room, not scanned.
 */
val NumeralLarge = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 46.sp,
    letterSpacing = (-1.5).sp
)

val NumeralMedium = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 26.sp,
    letterSpacing = (-0.5).sp
)

val NumeralSmall = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp
)

val AppTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 52.sp,
        letterSpacing = 1.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        letterSpacing = 0.8.sp
    )
)

/** Screen and panel titles on the character sheet. */
val RunicTitle = TextStyle(
    fontFamily = Cinzel,
    fontWeight = FontWeight.Bold,
    fontSize = 19.sp,
    letterSpacing = 1.4.sp
)

/** Panel headers: LOADOUT, ATTRIBUTES, QUEST LOG. */
val RunicLabel = TextStyle(
    fontFamily = Cinzel,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    letterSpacing = 1.8.sp
)

/** The level, and the attribute values beside it. */
val RunicNumeral = TextStyle(
    fontFamily = Cinzel,
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp
)

/** An attribute's value, beside its name. */
val RunicValue = TextStyle(
    fontFamily = Cinzel,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp
)

/** Rarity tags on item rows — small enough to sit at the end of a line. */
val RunicTag = TextStyle(
    fontFamily = Cinzel,
    fontWeight = FontWeight.SemiBold,
    fontSize = 9.sp,
    letterSpacing = 1.2.sp
)
