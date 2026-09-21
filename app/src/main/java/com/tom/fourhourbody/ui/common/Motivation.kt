package com.tom.fourhourbody.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tom.fourhourbody.ui.theme.NumeralSmall
import com.tom.fourhourbody.ui.theme.Palette

/**
 * The chain, as a row of days. A gap you can see is a stronger argument than a percentage —
 * this is the whole reason the strip exists.
 */
@Composable
fun WeekStrip(
    met: List<Boolean>,
    colour: Color,
    modifier: Modifier = Modifier,
    todayPending: Boolean = false
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        met.forEachIndexed { index, hit ->
            val isToday = todayPending && index == met.lastIndex
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .then(
                        when {
                            hit -> Modifier.background(colour)
                            isToday -> Modifier.border(1.dp, colour, CircleShape)
                            else -> Modifier.background(Palette.DotEmpty)
                        }
                    )
            )
        }
    }
}

/** Loss framing, with the forgiveness stated plainly so a slip does not feel terminal. */
@Composable
fun ChainPill(
    days: Int,
    passesLeft: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Palette.EmberSurface)
                .border(1.dp, Palette.EmberLine, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("$days", style = NumeralSmall, color = Palette.Ember)
            Text(
                "day chain",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.EmberText
            )
        }
        if (passesLeft > 0) {
            Spacer(Modifier.height(3.dp))
            Text(
                "$passesLeft pass left this week",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextTertiary
            )
        }
    }
}

/** A small ring for today's completion — immediate feedback on an otherwise invisible day. */
@Composable
fun CompletionRing(done: Int, total: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(44.dp)) {
            val strokeWidth = 5.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = Palette.DotEmpty,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth)
            )
            if (total > 0 && done > 0) {
                drawArc(
                    color = Palette.Ember,
                    startAngle = -90f,
                    sweepAngle = 360f * (done.toFloat() / total).coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            "$done",
            style = NumeralSmall,
            color = Palette.TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}

/** A pillar row that carries its own action, so nothing needs navigating to first. */
@Composable
fun ActionRow(
    colour: Color,
    title: String,
    detail: String,
    detailColour: Color = Palette.TextSecondary,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(colour)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = detailColour)
        }
        if (action != null) action()
    }
}
