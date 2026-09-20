package com.tom.fourhourbody.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/**
 * Trend line drawn on a Compose Canvas rather than pulled in from a chart library — two
 * single-series line charts do not justify the dependency, and the brief allows either.
 */
@Composable
fun TrendChart(
    title: String,
    points: List<Pair<LocalDate, Double>>,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (points.size < 2) {
            Text(
                "Two entries are needed before a trend means anything.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val values = points.map { it.second }
        val minValue = values.min()
        val maxValue = values.max()
        val span = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
        val lineColor = MaterialTheme.colorScheme.primary
        val gridColor = MaterialTheme.colorScheme.outlineVariant

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
            val path = Path()

            points.forEachIndexed { index, (_, value) ->
                val x = stepX * index
                val y = size.height - (((value - minValue) / span).toFloat() * size.height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawLine(
                color = gridColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f
            )
            drawPath(path = path, color = lineColor, style = Stroke(width = 5f))

            points.forEachIndexed { index, (_, value) ->
                val x = stepX * index
                val y = size.height - (((value - minValue) / span).toFloat() * size.height)
                drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "%.1f %s".format(minValue, unit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "%.1f %s".format(maxValue, unit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
