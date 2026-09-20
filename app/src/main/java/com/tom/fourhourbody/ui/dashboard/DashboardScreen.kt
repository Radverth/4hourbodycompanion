package com.tom.fourhourbody.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.data.repo.DashboardState
import com.tom.fourhourbody.domain.creatine.CreatineCycle
import com.tom.fourhourbody.ui.common.LabelledProgress
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.ui.nav.Routes
import com.tom.fourhourbody.util.displayShort

/**
 * Today's checklist across every enabled pillar, plus the adherence view. Everything here is a
 * local Room Flow — this is the screen opened most often and it has to be instant.
 */
@Composable
fun DashboardScreen(onOpenPillar: (String) -> Unit) {
    val container = rememberContainer()
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val adherence by viewModel.adherence.collectAsStateWithLifecycle()
    val window by viewModel.window.collectAsStateWithLifecycle()

    val current = state

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Today", style = MaterialTheme.typography.headlineMedium)
                if (current != null) {
                    Text(
                        current.date.displayShort(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (current == null) {
            item { Text("Loading…") }
            return@LazyColumn
        }

        pillarCards(current, onOpenPillar)

        item {
            Spacer(Modifier.height(8.dp))
            Text("Adherence", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30).forEach { days ->
                    FilterChip(
                        selected = window == days,
                        onClick = { viewModel.setWindow(days) },
                        label = { Text("Last $days days") }
                    )
                }
            }
        }

        items(adherence, key = { it.pillar.name }) { entry ->
            LabelledProgress(
                label = entry.pillar.label,
                detail = entry.detail,
                percent = entry.percent
            )
        }

        if (adherence.isEmpty()) {
            item {
                Text(
                    "No pillars enabled. Turn one on in Settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.pillarCards(
    state: DashboardState,
    onOpenPillar: (String) -> Unit
) {
    val settings = state.settings

    if (settings.isEnabled(Pillar.TRAINING)) {
        item {
            val training = state.training
            SectionCard(
                title = "Training",
                subtitle = when {
                    training.completedToday && training.lastSessionStalled ->
                        "Logged today — stalled, so the gap is now ${training.restDaysBetween} rest days."
                    training.completedToday -> "Session logged today."
                    training.dueToday -> "Session due today."
                    else -> training.nextSessionDate
                        ?.let { "Next session ${it.displayShort()} (${training.restDaysBetween} rest days)." }
                        ?: "No sessions logged yet."
                },
                onClick = { onOpenPillar(Routes.TRAINING) }
            )
        }
    }

    if (settings.isEnabled(Pillar.STRETCHES)) {
        item {
            val stretches = state.stretches
            SectionCard(
                title = "Static stretches",
                subtitle = buildString {
                    append(
                        if (stretches.inlineDoneThisSession) {
                            "Session stretches done today. "
                        } else {
                            "No session stretches today. "
                        }
                    )
                    append("Desk resets today: ${stretches.deskResetCountToday}. ")
                    append("Standalone mobility this week: ${stretches.mobilityThisWeek}.")
                },
                onClick = { onOpenPillar(Routes.STRETCHES) }
            )
        }
    }

    if (settings.isEnabled(Pillar.NUTRITION)) {
        item {
            val nutrition = state.nutrition
            SectionCard(
                title = "Nutrition",
                subtitle = when {
                    !nutrition.dayLogged -> "Day not logged yet."
                    nutrition.isCheatDay ->
                        "Cheat day — ${nutrition.damageControlTicks}/5 damage-control taps, " +
                            "${nutrition.mealsLogged} meals logged."
                    else ->
                        "${nutrition.rulesMet}/3 rules held, ${nutrition.mealsLogged} meals logged."
                },
                onClick = { onOpenPillar(Routes.NUTRITION) }
            )
        }
    }

    if (settings.isEnabled(Pillar.SLEEP)) {
        item {
            val sleep = state.sleep
            SectionCard(
                title = "Sleep",
                subtitle = if (sleep.logged) {
                    "Night of ${sleep.nightDate.displayShort()} — ${sleep.checksPassed}/5 checks."
                } else {
                    "Night of ${sleep.nightDate.displayShort()} not logged."
                },
                onClick = { onOpenPillar(Routes.SLEEP) }
            )
        }
    }

    if (settings.isEnabled(Pillar.COLD)) {
        item {
            SectionCard(
                title = "Cold exposure",
                subtitle = "${state.cold.countThisWeek} this week.",
                onClick = { onOpenPillar(Routes.COLD) }
            )
        }
    }

    if (settings.isEnabled(Pillar.CREATINE)) {
        item {
            val creatine = state.creatine
            SectionCard(
                title = "Creatine",
                subtitle = when {
                    creatine.cycleDay != null ->
                        "Day ${creatine.cycleDay} of ${CreatineCycle.CYCLE_LENGTH_DAYS} — " +
                            "morning ${creatine.morningTaken.tick()}, evening ${creatine.eveningTaken.tick()}."
                    creatine.cycleComplete -> "Cycle complete. Start a new one in Settings."
                    else -> "No cycle running. Set a start date in Settings."
                },
                onClick = { onOpenPillar(Routes.CREATINE) }
            )
        }
    }
}

private fun Boolean.tick(): String = if (this) "done" else "not yet"
