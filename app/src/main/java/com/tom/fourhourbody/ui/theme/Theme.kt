package com.tom.fourhourbody.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Slate = Color(0xFF1B3A4B)
private val SlateLight = Color(0xFF4A6C7E)
private val Rust = Color(0xFFB4530A)
private val Sand = Color(0xFFE9DFD2)

private val LightScheme = lightColorScheme(
    primary = Slate,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE0E9),
    onPrimaryContainer = Color(0xFF07202C),
    secondary = Rust,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC5),
    onSecondaryContainer = Color(0xFF331200),
    surfaceVariant = Sand
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9FCBE0),
    onPrimary = Color(0xFF07202C),
    primaryContainer = SlateLight,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFFB68A),
    onSecondary = Color(0xFF4A1D00),
    secondaryContainer = Color(0xFF6B2F04),
    onSecondaryContainer = Color(0xFFFFDCC5)
)

@Composable
fun FourHourBodyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
