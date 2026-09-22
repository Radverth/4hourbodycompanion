package com.tom.fourhourbody.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.tom.fourhourbody.data.entity.DietMode
import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.domain.creatine.CreatineCycle
import com.tom.fourhourbody.domain.shift.ShiftWeek
import com.tom.fourhourbody.ui.common.BackTopBar
import com.tom.fourhourbody.ui.common.NumberField
import com.tom.fourhourbody.ui.common.ChipRow
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.SwitchRow
import com.tom.fourhourbody.ui.theme.Palette
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.util.asTimeOfDay
import com.tom.fourhourbody.util.displayShort
import java.time.DayOfWeek
import java.time.LocalDate
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
                    title = "Shift pattern",
                    subtitle = "Reminders are kept out of your working hours. A prompt you " +
                        "can't act on just teaches you to ignore the app."
                ) {
                    Column {
                        SwitchRow(
                            label = "I work an alternating rota",
                            checked = settings.shiftEnabled,
                            onCheckedChange = { value ->
                                viewModel.update { it.copy(shiftEnabled = value) }
                            }
                        )

                        if (settings.shiftEnabled) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (settings.shiftAnchorMonday == null) {
                                    "Which week are you on right now?"
                                } else {
                                    when (viewModel.currentWeek(settings)) {
                                        ShiftWeek.A -> "This week is the early shift."
                                        ShiftWeek.B -> "This week is the late shift."
                                        null -> "Which week are you on right now?"
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Palette.TextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.anchorThisWeek(true) }) {
                                    Text("This week is early")
                                }
                                OutlinedButton(onClick = { viewModel.anchorThisWeek(false) }) {
                                    Text("This week is late")
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text("Early week", style = MaterialTheme.typography.titleMedium)
                            TimeRow(
                                label = "Starts",
                                minutes = settings.shiftAStartMinutes,
                                onChange = { v -> viewModel.update { it.copy(shiftAStartMinutes = v) } }
                            )
                            TimeRow(
                                label = "Ends",
                                minutes = settings.shiftAEndMinutes,
                                onChange = { v -> viewModel.update { it.copy(shiftAEndMinutes = v) } }
                            )

                            Spacer(Modifier.height(8.dp))
                            Text("Late week", style = MaterialTheme.typography.titleMedium)
                            TimeRow(
                                label = "Starts",
                                minutes = settings.shiftBStartMinutes,
                                onChange = { v -> viewModel.update { it.copy(shiftBStartMinutes = v) } }
                            )
                            TimeRow(
                                label = "Ends",
                                minutes = settings.shiftBEndMinutes,
                                onChange = { v -> viewModel.update { it.copy(shiftBEndMinutes = v) } }
                            )

                            Spacer(Modifier.height(8.dp))
                            Text("Working days", style = MaterialTheme.typography.bodyMedium)
                            DayPicker(
                                selected = settings.workDays,
                                onToggle = { day ->
                                    viewModel.update { current ->
                                        current.copy(workDays = current.workDays.toggle(day))
                                    }
                                }
                            )

                            Spacer(Modifier.height(8.dp))
                            SwitchRow(
                                label = "I can stretch at my desk",
                                checked = settings.canStretchAtWork,
                                onCheckedChange = { value ->
                                    viewModel.update { it.copy(canStretchAtWork = value) }
                                },
                                supporting = if (settings.canStretchAtWork) {
                                    "Desk resets are spread through the working day."
                                } else {
                                    "Desk resets wait until your shift ends."
                                }
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(
                    title = "When will you do it?",
                    subtitle = "Saying when and where roughly doubles the odds of following " +
                        "through. The app reads this back at the moment it matters."
                ) {
                    Column {
                        Pillar.entries.filter(settings::isEnabled).forEach { pillar ->
                            OutlinedTextField(
                                value = settings.intentionFor(pillar).orEmpty(),
                                onValueChange = { viewModel.setIntention(pillar, it) },
                                label = { Text(pillar.label) },
                                placeholder = { Text("train right after my shift") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                SectionCard(
                    title = "Pillars",
                    subtitle = "A pillar switched off leaves the dashboard and stops its " +
                        "reminders. Nothing already logged is deleted."
                ) {
                    Column {
                        Pillar.entries.forEach { pillar ->
                            SwitchRow(
                                label = pillar.label,
                                checked = settings.isEnabled(pillar),
                                onCheckedChange = { viewModel.setPillar(pillar, it) }
                            )
                        }
                    }
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
                        Spacer(Modifier.height(8.dp))
                        Text("Preferred training days", style = MaterialTheme.typography.bodyMedium)
                        DayPicker(
                            selected = settings.trainingDays,
                            onToggle = { day ->
                                viewModel.update { current ->
                                    current.copy(
                                        trainingDays = current.trainingDays.toggle(day)
                                    )
                                }
                            }
                        )
                        Text(
                            "Advisory only — the real gap comes from the stall rule, currently " +
                                "adjusted automatically after every stalled session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        SwitchRow(
                            label = "Six-Minute Abs block",
                            checked = settings.sixMinuteAbsEnabled,
                            onCheckedChange = { value ->
                                viewModel.update { it.copy(sixMinuteAbsEnabled = value) }
                            }
                        )
                        NumberField(
                            label = "Default bell weight (kg)",
                            value = settings.defaultBellWeightKg.toString(),
                            onValueChange = { raw ->
                                raw.toDoubleOrNull()?.let { value ->
                                    viewModel.update { it.copy(defaultBellWeightKg = value) }
                                }
                            },
                            decimal = true,
                            modifier = Modifier.width(200.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        SwitchRow(
                            label = "Show the locked-position cue again",
                            checked = !settings.lockedPositionCueDismissed,
                            onCheckedChange = { value ->
                                viewModel.update { it.copy(lockedPositionCueDismissed = !value) }
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
                SectionCard(title = "Nutrition") {
                    ChipRow {
                        DietMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.dietMode == mode,
                                onClick = { viewModel.update { it.copy(dietMode = mode) } },
                                label = {
                                    Text(if (mode == DietMode.SLOW_CARB) "Slow-Carb" else "Hybrid")
                                }
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Static stretches") {
                    Column {
                        SwitchRow(
                            label = "Desk-reset reminders",
                            checked = settings.deskResetRemindersEnabled,
                            onCheckedChange = { value ->
                                viewModel.update { it.copy(deskResetRemindersEnabled = value) }
                            },
                            supporting = "Every few hours during the work-hours window."
                        )
                        Text(
                            "Used only when the shift rota is off.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.TextSecondary
                        )
                        TimeRow(
                            label = "Window starts",
                            minutes = settings.deskResetStartMinutes,
                            onChange = { value -> viewModel.update { it.copy(deskResetStartMinutes = value) } }
                        )
                        TimeRow(
                            label = "Window ends",
                            minutes = settings.deskResetEndMinutes,
                            onChange = { value -> viewModel.update { it.copy(deskResetEndMinutes = value) } }
                        )
                        NumberField(
                            label = "Interval (hours)",
                            value = settings.deskResetIntervalHours.toString(),
                            onValueChange = { raw ->
                                raw.toIntOrNull()?.takeIf { it in 1..12 }?.let { value ->
                                    viewModel.update { it.copy(deskResetIntervalHours = value) }
                                }
                            },
                            modifier = Modifier.width(160.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Work days", style = MaterialTheme.typography.bodyMedium)
                        DayPicker(
                            selected = settings.workDays,
                            onToggle = { day ->
                                viewModel.update { current ->
                                    current.copy(workDays = current.workDays.toggle(day))
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Weekly desk reset", style = MaterialTheme.typography.bodyMedium)
                        DayPicker(
                            selected = setOf(settings.weeklyDeskResetDay),
                            onToggle = { day -> viewModel.update { it.copy(weeklyDeskResetDay = day) } }
                        )
                        Text("Rest-day mobility", style = MaterialTheme.typography.bodyMedium)
                        DayPicker(
                            selected = setOf(settings.weeklyMobilityDay),
                            onToggle = { day -> viewModel.update { it.copy(weeklyMobilityDay = day) } }
                        )
                        TimeRow(
                            label = "Weekly routine time",
                            minutes = settings.weeklyRoutineTimeMinutes,
                            onChange = { value ->
                                viewModel.update { it.copy(weeklyRoutineTimeMinutes = value) }
                            }
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Sleep") {
                    TimeRow(
                        label = "Checklist reminder",
                        minutes = settings.sleepReminderMinutes,
                        onChange = { value -> viewModel.update { it.copy(sleepReminderMinutes = value) } }
                    )
                }
            }

            item {
                SectionCard(
                    title = "Cold exposure",
                    subtitle = "Nice-to-have, so the reminder is off by default."
                ) {
                    Column {
                        SwitchRow(
                            label = "Reminders",
                            checked = settings.coldRemindersEnabled,
                            onCheckedChange = { value ->
                                viewModel.update { it.copy(coldRemindersEnabled = value) }
                            }
                        )
                        DayPicker(
                            selected = settings.coldReminderDays,
                            onToggle = { day ->
                                viewModel.update { current ->
                                    current.copy(coldReminderDays = current.coldReminderDays.toggle(day))
                                }
                            }
                        )
                        TimeRow(
                            label = "Time",
                            minutes = settings.coldReminderMinutes,
                            onChange = { value -> viewModel.update { it.copy(coldReminderMinutes = value) } }
                        )
                    }
                }
            }

            item {
                SectionCard(
                    title = "Creatine",
                    subtitle = settings.creatineCycleStartDate?.let {
                        "Cycle started ${it.displayShort()}, " +
                            "${CreatineCycle.CYCLE_LENGTH_DAYS} days."
                    } ?: "No cycle running."
                ) {
                    Column {
                        SwitchRow(
                            label = "Twice-daily reminders",
                            checked = settings.creatineRemindersEnabled,
                            onCheckedChange = { value ->
                                viewModel.update { it.copy(creatineRemindersEnabled = value) }
                            }
                        )
                        TimeRow(
                            label = "Morning",
                            minutes = settings.creatineMorningMinutes,
                            onChange = { value ->
                                viewModel.update { it.copy(creatineMorningMinutes = value) }
                            }
                        )
                        TimeRow(
                            label = "Evening",
                            minutes = settings.creatineEveningMinutes,
                            onChange = { value ->
                                viewModel.update { it.copy(creatineEveningMinutes = value) }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        SwitchRow(
                            label = "Cycle running from today",
                            checked = settings.creatineCycleStartDate != null,
                            onCheckedChange = { value ->
                                viewModel.update {
                                    it.copy(
                                        creatineCycleStartDate = if (value) LocalDate.now() else null
                                    )
                                }
                            }
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

private fun Set<DayOfWeek>.toggle(day: DayOfWeek): Set<DayOfWeek> =
    if (contains(day)) this - day else this + day
