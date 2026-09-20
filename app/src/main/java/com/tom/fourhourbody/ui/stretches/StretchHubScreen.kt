package com.tom.fourhourbody.ui.stretches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.data.entity.StretchRoutine
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.util.asClock

@Composable
fun StretchHubScreen(
    onOpenMobility: () -> Unit,
    onOpenDeskReset: (Boolean) -> Unit,
    onOpenConfig: () -> Unit
) {
    val container = rememberContainer()
    val viewModel: StretchConfigViewModel =
        viewModel(factory = StretchConfigViewModel.factory(container))
    val configs by viewModel.configs.collectAsStateWithLifecycle()
    val todaysLogs by viewModel.todaysLogs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Static stretches", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Text(
                "Held positions, 10 seconds or more — a separate practice from the dynamic " +
                    "warm-up drills, and treated as its own pillar here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            SectionCard(
                title = "Desk reset",
                subtitle = "Static Back, Static Extension, Shoulder Bridge. Every 2–3 hours at a desk.",
                onClick = { onOpenDeskReset(false) }
            )
        }
        item {
            SectionCard(
                title = "Weekly desk reset",
                subtitle = "The full set, including Active Bridges, Supine Groin Progressive and Air Bench.",
                onClick = { onOpenDeskReset(true) }
            )
        }
        item {
            SectionCard(
                title = "Rest-day mobility",
                subtitle = "Super quad, pelvic symmetry, pelvis repositioning.",
                onClick = onOpenMobility
            )
        }
        item {
            SectionCard(
                title = "Edit hold times and rep targets",
                subtitle = "Changes here apply everywhere the stretch is used, inline or standalone.",
                onClick = onOpenConfig
            )
        }

        item {
            Text("Logged today", style = MaterialTheme.typography.titleMedium)
        }

        if (todaysLogs.isEmpty()) {
            item {
                Text(
                    "Nothing yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(todaysLogs, key = { it.id }) { log ->
            val detail = buildString {
                append(log.sideOrPosition)
                append(" · ")
                append(log.repsCompleted?.let { "$it reps" } ?: log.durationSec.asClock())
                append(if (log.sessionId != null) " · in session" else " · standalone")
            }
            Text("${log.stretchName} — $detail", style = MaterialTheme.typography.bodyMedium)
        }

        item {
            val byRoutine = configs.groupBy { it.routine }
            Text(
                "Configured: " + StretchRoutine.entries.joinToString(", ") { routine ->
                    "${routine.label} ${byRoutine[routine]?.count { it.isActive } ?: 0}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
