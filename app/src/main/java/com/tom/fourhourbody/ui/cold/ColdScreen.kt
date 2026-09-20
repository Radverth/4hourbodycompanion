package com.tom.fourhourbody.ui.cold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.data.entity.ColdExposureType
import com.tom.fourhourbody.ui.common.NumberField
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.util.MonotonicTimer
import com.tom.fourhourbody.util.asClock
import com.tom.fourhourbody.util.displayShort

/** Log-it-and-move-on. This pillar is nice-to-have, so nothing here nags. */
@Composable
fun ColdScreen() {
    val container = rememberContainer()
    val viewModel: ColdViewModel = viewModel(factory = ColdViewModel.factory(container))
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val countThisWeek by viewModel.countThisWeek.collectAsStateWithLifecycle()

    var type by remember { mutableStateOf(ColdExposureType.SHOWER) }
    var manualSeconds by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var timedSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val base = elapsedMs
        MonotonicTimer.elapsedFlow().collect { tick ->
            elapsedMs = base + tick
            timedSeconds = (elapsedMs / 1000L).toInt()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Cold exposure", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "$countThisWeek this week.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SectionCard(title = "Type") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColdExposureType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.label) }
                        )
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Duration",
                subtitle = "Run the timer, or just type it in afterwards."
            ) {
                Column {
                    Text(timedSeconds.asClock(), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { running = !running }) {
                            Text(if (running) "Pause" else "Start")
                        }
                        OutlinedButton(
                            onClick = {
                                running = false
                                elapsedMs = 0L
                                timedSeconds = 0
                            }
                        ) { Text("Reset") }
                    }
                    Spacer(Modifier.height(12.dp))
                    NumberField(
                        label = "Or enter seconds",
                        value = manualSeconds,
                        onValueChange = { manualSeconds = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val seconds = manualSeconds.toIntOrNull() ?: timedSeconds
                            if (seconds > 0) {
                                viewModel.log(type, seconds, null)
                                running = false
                                elapsedMs = 0L
                                timedSeconds = 0
                                manualSeconds = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save") }
                }
            }
        }

        item { Text("Recent", style = MaterialTheme.typography.titleMedium) }

        items(recent, key = { it.id }) { log ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(log.type.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${log.date.displayShort()} · ${log.durationSec.asClock()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { viewModel.delete(log) }) { Text("Delete") }
            }
        }
    }
}
