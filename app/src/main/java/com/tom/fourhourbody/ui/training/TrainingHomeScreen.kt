package com.tom.fourhourbody.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.domain.training.TrainingConstants
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.util.displayShort

@Composable
fun TrainingHomeScreen(
    onStartSession: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenExercises: () -> Unit
) {
    val container = rememberContainer()
    val viewModel: TrainingViewModel = viewModel(factory = TrainingViewModel.factory(container))
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Training", style = MaterialTheme.typography.headlineMedium) }

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
                title = "History",
                subtitle = "${sessions.count { it.completed }} completed sessions.",
                onClick = onOpenHistory
            )
        }

        item { Text("Recent", style = MaterialTheme.typography.titleMedium) }

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
