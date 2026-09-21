package com.tom.fourhourbody.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.domain.run.RunSummary
import com.tom.fourhourbody.domain.training.TrainingConstants
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.ui.theme.NumeralMedium
import com.tom.fourhourbody.ui.theme.NumeralSmall
import com.tom.fourhourbody.ui.theme.Palette
import com.tom.fourhourbody.util.displayShort
import com.tom.fourhourbody.util.kgDisplay

@Composable
fun TrainingHomeScreen(
    onStartSession: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenExercises: () -> Unit,
    onOpenDeck: () -> Unit
) {
    val container = rememberContainer()
    val viewModel: TrainingViewModel = viewModel(factory = TrainingViewModel.factory(container))
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val runs by viewModel.runs.collectAsStateWithLifecycle()
    val runStatus by viewModel.runStatus.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Training", style = MaterialTheme.typography.headlineMedium) }

        item {
            val open = runs.firstOrNull()?.takeIf { !it.isComplete }
            when {
                open != null -> CurrentRunStrip(open)
                runStatus != null -> NotStartedRunStrip(
                    runNumber = runStatus!!.runNumber,
                    restDays = schedule?.restDaysBetween,
                    onStartSession = onStartSession
                )
            }
        }

        item {
            val current = schedule
            SectionCard(
                title = when {
                    current == null -> "Loading…"
                    current.dueToday -> "Session due today"
                    else -> "Next session in ${current.daysUntilNext} days"
                },
                subtitle = current?.let {
                    buildString {
                        append("Rest days between sessions: ${it.restDaysBetween}. ")
                        append(
                            it.nextSessionDate
                                ?.let { date -> "Due from ${date.displayShort()}." }
                                ?: "No sessions logged yet."
                        )
                    }
                }
            ) {
                Button(onClick = onStartSession, modifier = Modifier.fillMaxWidth()) {
                    Text("Start session")
                }
            }
        }

        item {
            SectionCard(
                title = "Protocol",
                subtitle = "Target ${TrainingConstants.DEFAULT_TARGET_REPS}+ reps to failure " +
                    "(${TrainingConstants.LEG_PRESS_TARGET_REPS}+ on leg press), " +
                    "${TrainingConstants.TEMPO_UP_SEC}s up / ${TrainingConstants.TEMPO_DOWN_SEC}s " +
                    "down, ${TrainingConstants.REST_BETWEEN_EXERCISES_SEC / 60} minutes between " +
                    "exercises. A miss of more than one rep ends the session and adds a rest day."
            )
        }

        item {
            SectionCard(
                title = "Exercises",
                subtitle = "Slots, equipment and rep targets.",
                onClick = onOpenExercises
            )
        }

        item {
            SectionCard(
                title = "Your deck",
                subtitle = "What every exercise and protocol has earned so far.",
                onClick = onOpenDeck
            )
        }

        item {
            SectionCard(
                title = "History",
                subtitle = "${sessions.count { it.completed }} completed sessions.",
                onClick = onOpenHistory
            )
        }

        val finished = runs.filter { it.isComplete }
        if (finished.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Past runs", style = MaterialTheme.typography.titleMedium)
                Text(
                    "+${finished.sumOf { it.totalGainKg }.kgDisplay()} banked across " +
                        "${finished.size} ${if (finished.size == 1) "run" else "runs"}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary
                )
            }
            items(finished, key = { it.runNumber }) { run -> FinishedRunRow(run) }
        }

        item { Text("Recent sessions", style = MaterialTheme.typography.titleMedium) }

        items(sessions.take(5), key = { it.id }) { session ->
            Column(Modifier.fillMaxWidth()) {
                Text(session.date.displayShort(), style = MaterialTheme.typography.bodyLarge)
                Text(
                    when {
                        !session.completed -> "Abandoned"
                        session.stalled -> "Stalled — rest days increased"
                        else -> "Completed"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * The run in progress. It shows only what has already been logged — sessions done and weight
 * added so far — so the strip is a record of the block, never a target to chase.
 */
@Composable
private fun CurrentRunStrip(run: RunSummary) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.EmberSurface)
            .border(1.dp, Palette.EmberLine, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Text("RUN IN PROGRESS", style = MaterialTheme.typography.labelSmall, color = Palette.EmberText)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("RUN ${run.runNumber}", style = NumeralMedium, color = Palette.Ember)
            Text(
                "  ${run.sessions} ${if (run.sessions == 1) "session" else "sessions"} · " +
                    "day ${run.days}",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (run.totalGainKg > 0) {
                "+${run.totalGainKg.kgDisplay()} added so far, on ${run.restDaysBefore} days rest."
            } else {
                "Running on ${run.restDaysBefore} days rest. Nothing banked yet."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.EmberText
        )
    }
}

@Composable
private fun FinishedRunRow(run: RunSummary) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Run ${run.runNumber}", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${run.sessions} ${if (run.sessions == 1) "session" else "sessions"} · " +
                    "${run.days} days" +
                    (run.stalledOn?.let { " · stalled on $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary
            )
        }
        Text("+${run.totalGainKg.kgDisplay()}", style = NumeralSmall, color = Palette.Ember)
    }
}

/**
 * No run is open. Rendering nothing here was the whole problem: the app is built around runs
 * and, until one had been started and then stalled, it never said the word. A run that has
 * not begun still has a number and still says what it is for.
 */
@Composable
private fun NotStartedRunStrip(
    runNumber: Int,
    restDays: Int?,
    onStartSession: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.EmberSurface)
            .border(1.dp, Palette.EmberLine, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Text(
            "NEXT RUN",
            style = MaterialTheme.typography.labelSmall,
            color = Palette.EmberText
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("RUN $runNumber", style = NumeralMedium, color = Palette.Ember)
            Text(
                "  not started",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            restDays?.let {
                "It opens on your next session and runs on $it days rest, closing when you " +
                    "first miss a target by more than a rep. Every weight it earns is kept."
            } ?: "It opens on your first session and closes when you first miss a target by " +
                "more than a rep. Every weight it earns is kept.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.EmberText
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onStartSession, modifier = Modifier.fillMaxWidth()) {
            Text("Start run $runNumber")
        }
    }
}
