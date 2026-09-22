package com.tom.fourhourbody.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.BuildConfig
import com.tom.fourhourbody.ui.common.BackTopBar
import com.tom.fourhourbody.ui.common.ChipRow
import com.tom.fourhourbody.ui.common.NumberField
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.SwitchRow
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.util.asTimeOfDay
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = rememberContainer()
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar("Settings", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(
                    title = "When will you train?",
                    subtitle = "Saying when and where roughly doubles the odds of following " +
                        "through. The app reads this back at the moment it matters."
                ) {
                    OutlinedTextField(
                        value = settings.trainingIntention.orEmpty(),
                        onValueChange = { viewModel.setIntention(it) },
                        label = { Text("Training") },
                        placeholder = { Text("train right after work") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                SectionCard(title = "Training") {
                    Column {
                        TimeRow(
                            label = "Session reminder",
                            minutes = settings.reminderTimeMinutes,
                            onChange = { value -> viewModel.update { it.copy(reminderTimeMinutes = value) } }
                        )
                        SwitchRow(
                            label = "Show the first-set cue again",
                            checked = !settings.firstSetCueDismissed,
                            onCheckedChange = { value ->
                                viewModel.update { it.copy(firstSetCueDismissed = !value) }
                            }
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Weigh-in") {
                    Column {
                        Text("Day", style = MaterialTheme.typography.bodyMedium)
                        DayPicker(
                            selected = setOf(settings.weighInDay),
                            onToggle = { day -> viewModel.update { it.copy(weighInDay = day) } }
                        )
                        TimeRow(
                            label = "Time",
                            minutes = settings.weighInTimeMinutes,
                            onChange = { value -> viewModel.update { it.copy(weighInTimeMinutes = value) } }
                        )
                    }
                }
            }

            item {
                SectionCard(
                    title = "About",
                    subtitle = "Version ${BuildConfig.VERSION_NAME}, code " +
                        "${BuildConfig.VERSION_CODE}. Worth quoting in any bug report — it " +
                        "names the exact build, which a screenshot otherwise cannot."
                )
            }
        }
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                minutes.asTimeOfDay(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        NumberField(
            label = "Hour",
            value = (minutes / 60).toString(),
            onValueChange = { raw ->
                raw.toIntOrNull()?.takeIf { it in 0..23 }?.let { onChange(it * 60 + minutes % 60) }
            },
            modifier = Modifier.width(90.dp)
        )
        NumberField(
            label = "Min",
            value = (minutes % 60).toString(),
            onValueChange = { raw ->
                raw.toIntOrNull()?.takeIf { it in 0..59 }?.let { onChange((minutes / 60) * 60 + it) }
            },
            modifier = Modifier.width(90.dp)
        )
    }
}

@Composable
private fun DayPicker(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    ChipRow {
        weekDays.forEach { day ->
            FilterChip(
                selected = selected.contains(day),
                onClick = { onToggle(day) },
                label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2)) }
            )
        }
    }
}

private val weekDays: List<DayOfWeek> = DayOfWeek.values().toList()
