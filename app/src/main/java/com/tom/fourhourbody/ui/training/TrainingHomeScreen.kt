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
import androidx.compose.material3.OutlinedButton
import com.tom.fourhourbody.data.entity.SessionKind
import com.tom.fourhourbody.domain.run.RunSummary
import com.tom.fourhourbody.domain.training.SessionScheduler
import com.tom.fourhourbody.domain.training.TrainingConstants
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.SwitchRow
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.ui.theme.NumeralMedium
import com.tom.fourhourbody.ui.theme.NumeralSmall
import com.tom.fourhourbody.ui.theme.Palette
import com.tom.fourhourbody.util.displayShort
import com.tom.fourhourbody.util.kgDisplay

@Composable
fun TrainingHomeScreen(
    onStartSession: (SessionKind) -> Unit,
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
    val frequency by viewModel.frequency.collectAsStateWithLifecycle()
    val plateau by viewModel.plateau.collectAsStateWithLifecycle()
    val cutting = frequency?.cuttingPhaseActive == true

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
                    onStartSession = { onStartSession(SessionKind.STANDARD) }
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
                        append("${it.restDaysBetween} rest days between sessions")
                        // The gap widening is the protocol working. Saying so matters: a
                        // number that only ever grows looks like a tracker reporting decline
                        // unless something explains that growing is the intended direction.
                        append(
                            when {
                                SessionScheduler.isUnusuallyLongGap(it.restDaysBetween) ->
                                    " — a long way out. The book is clear that ten to " +
                                        "fourteen days costs nothing, so this is still the " +
                                        "mechanism working; past that it is worth a look at " +
                                        "whether something else is going on."
                                it.restDaysBetween > TrainingConstants.INITIAL_REST_DAYS ->
                                    " — wider than the starting gap, which is the protocol " +
                                        "working. A set taken to failure takes longer to " +
                                        "recover from the stronger you get."
                                else -> ", the protocol's starting point."
                            }
                        )
                        append(" ")
                        append(
                            it.nextSessionDate
                                ?.let { date -> "Due from ${date.displayShort()}." }
                                ?: "No sessions logged yet."
                        )
                    }
                }
            ) {
                Button(
                    onClick = { onStartSession(SessionKind.STANDARD) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (cutting) "Start cutting session" else "Start session")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onStartSession(SessionKind.NO_EQUIPMENT) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No equipment tonight")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Board and bodyweight, same clock and same rules. On the board a handle " +
                        "position is the load, so a position only changes once it has cleared " +
                        "${TrainingConstants.TARGET_TUL_MAX_SEC}s twice running.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary
                )
            }
        }

        item {
            SectionCard(
                title = "Cutting phase",
                subtitle = "Leg press plus one alternating upper-body exercise — chest press " +
                    "one session, seated row the next."
            ) {
                SwitchRow(
                    label = if (cutting) "On" else "Off",
                    checked = cutting,
                    onCheckedChange = viewModel::setCuttingPhase
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "For a sustained calorie deficit, not a single session. The book's own " +
                        "fat-loss study found that cutting volume down during a deficit kept " +
                        "twice the muscle and lost twice the fat, because dieting is already " +
                        "spending the recovery the training needs. Your slot list is left " +
                        "exactly as it is — this only changes which exercises a session picks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary
                )
            }
        }

        plateau?.let { stuck ->
            item {
                PlateauOffer(
                    plateau = stuck,
                    onTechniqueLogged = { viewModel.logPlateauTechnique(stuck.exerciseName, it) }
                )
            }
        }

        item {
            SectionCard(
                title = "Protocol",
                subtitle = "One set per exercise to positive failure at " +
                    "${TrainingConstants.TEMPO_UP_SEC}s up / " +
                    "${TrainingConstants.TEMPO_DOWN_SEC}s down, aiming for " +
                    "${TrainingConstants.TARGET_TUL_MIN_SEC}–" +
                    "${TrainingConstants.TARGET_TUL_MAX_SEC} seconds under load, " +
                    "${TrainingConstants.REST_BETWEEN_EXERCISES_SEC}s between exercises. " +
                    "Clearing ${TrainingConstants.TARGET_TUL_MAX_SEC}s earns 5–10% more load. " +
                    "Failing to match the same load's last time ends the session and adds a " +
                    "rest day."
            )
        }

        item {
            SectionCard(
                title = "Exercises",
                subtitle = "Slots, equipment and the seconds each is aiming for.",
                onClick = onOpenExercises
            )
        }

        item {
            SectionCard(
                title = "Character sheet",
                subtitle = "Attributes, loadout and the quest log.",
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
