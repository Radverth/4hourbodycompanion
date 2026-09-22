package com.tom.fourhourbody.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Dark only, and deliberately not dynamic colour: the ember accent has a job — it is the
 * one colour that means "act now" — and letting the wallpaper reassign it would break the
 * hierarchy the whole screen rests on.
 */
private val AppScheme = darkColorScheme(
    primary = Palette.Ember,
    onPrimary = Palette.EmberInk,
    primaryContainer = Palette.EmberSurface,
    onPrimaryContainer = Palette.TextPrimary,
    secondary = Palette.Secondary,
    onSecondary = Palette.EmberInk,
    background = Palette.Ground,
    onBackground = Palette.TextPrimary,
    surface = Palette.Surface,
    onSurface = Palette.TextPrimary,
    surfaceVariant = Palette.SurfaceRaised,
    onSurfaceVariant = Palette.TextSecondary,
    outline = Palette.LineStrong,
    outlineVariant = Palette.Line,
    error = Palette.Warn,
    onError = Palette.EmberInk
)

@Composable
fun FourHourBodyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppScheme,
        typography = AppTypography,
        content = content
    )
}
