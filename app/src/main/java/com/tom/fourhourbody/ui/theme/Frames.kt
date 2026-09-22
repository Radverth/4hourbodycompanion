package com.tom.fourhourbody.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A panel with its top-left and bottom-right corners cut away.
 *
 * Rounded corners are the language of the rest of the app. The character sheet wants the
 * bevelled plate an action RPG uses instead, and the difference carries meaning: one kind of
 * screen is acted on, the other is inspected.
 */
class NotchedShape(private val notch: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val n = with(density) { notch.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2f)
        return Outline.Generic(
            Path().apply {
                moveTo(n, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - n)
                lineTo(size.width - n, size.height)
                lineTo(0f, size.height)
                lineTo(0f, n)
                close()
            }
        )
    }
}

val PanelShape = NotchedShape(9.dp)
val CalloutShape = NotchedShape(11.dp)
val ForgeButtonShape = NotchedShape(8.dp)
