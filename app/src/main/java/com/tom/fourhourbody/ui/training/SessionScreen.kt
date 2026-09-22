package com.tom.fourhourbody.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.domain.run.RunSummary
import com.tom.fourhourbody.domain.training.TrainingConstants
import com.tom.fourhourbody.ui.common.CheckRow
import com.tom.fourhourbody.ui.common.KeepScreenOn
import com.tom.fourhourbody.ui.common.NumberField
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.ui.theme.NumeralLarge
import com.tom.fourhourbody.ui.theme.NumeralMedium
import com.tom.fourhourbody.ui.theme.NumeralSmall
import com.tom.fourhourbody.ui.theme.Palette
import com.tom.fourhourbody.util.kgDisplay

/**
 * The guided Big Five session: a one-time cue, then five exercises, each one set to positive
 * failure timed on a stopwatch, with a brisk timed transition between.
 */
@Composable
fun SessionScreen(onExit: () -> Unit) {
    val container = rememberContainer()
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModel.factory(container))
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()

    KeepScreenOn()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val current = stage) {
            SessionStage.Loading -> Text("Preparing session…")

            SessionStage.FirstSetCue -> FirstSetCueStage(
                onContinue = viewModel::acknowledgeFirstSetCue
            )

            is SessionStage.Strength -> {
                val currentPrompt = prompt
                if (currentPrompt == null) {
                    Text("Loading exercise…")
                } else {
                    StrengthStage(
                        prompt = currentPrompt,
                        onLog = { weight, tulSec ->
                            viewModel.logExercise(current.index, weight, tulSec)
                        }
                    )
                }
            }

            is SessionStage.Rest -> {
                Text("Rest", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Move briskly to the next machine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CountdownBlock(
                    totalSec = TrainingConstants.REST_BETWEEN_EXERCISES_SEC,
                    label = "until the next exercise",
                    autoStart = true,
                    onFinished = { restTaken ->
                        viewModel.onRestFinished(restTaken, current.nextIndex)
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ready early? Skip logs the rest you actually took.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is SessionStage.Summary -> SummaryStage(
                stage = current,
                onDone = onExit
            )
        }

        if (stage !is SessionStage.Summary) {
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onExit) { Text("Leave session") }
        }
    }
}

@Composable
private fun FirstSetCueStage(onContinue: (Boolean) -> Unit) {
    var dontShowAgain by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Before you start", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(TrainingConstants.FIRST_SET_CUE, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            CheckRow(
                label = "Don't show this again",
                checked = dontShowAgain,
                onCheckedChange = { dontShowAgain = it }
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onContinue(dontShowAgain) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start")
            }
        }
    }
}

@Composable
private fun StrengthStage(prompt: ExercisePrompt, onLog: (Double, Int) -> Unit) {
    var weightText by remember(prompt.config.id) {
        mutableStateOf(prompt.suggestedWeightKg?.let { "%.1f".format(it) } ?: "")
    }

    Text(
        "Exercise ${prompt.position} of ${prompt.total}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(prompt.config.exerciseName, style = MaterialTheme.typography.headlineSmall)
    Text(
        "One set to positive failure · ${prompt.config.equipment}",
        style = MaterialTheme.typography.bodyMedium
    )

    if (prompt.lastWeightKg != null && prompt.lastTulSec != null) {
        Text(
            "Last time: ${prompt.lastWeightKg.kgDisplay()} for ${prompt.lastTulSec}s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (prompt.suggestedWeightKg != null) {
        Text(
            "Suggested: ${prompt.suggestedWeightKg.kgDisplay()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val entered = weightText.toDoubleOrNull()
    if (entered != null && prompt.isRecord(entered)) {
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Palette.EmberSurface)
                .border(1.dp, Palette.EmberLine, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "NEW BEST",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.Ember
            )
            Text(
                "  past ${prompt.bestEverKg!!.kgDisplay()}",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.EmberText
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    NumberField(
        label = "Weight (kg)",
        value = weightText,
        onValueChange = { weightText = it },
        decimal = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(16.dp))
    val weight = weightText.toDoubleOrNull()
    if (weight != null) {
        WorkingSetTimer(onFailure = { tulSec -> onLog(weight, tulSec) })
    } else {
        Text(
            "Enter a weight to start the set.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * How a session ends is what is remembered of it, so it ends on the reward rather than on a
 * receipt. A session inside a run ends on the weights it just earned for next time; a session
 * whose plateau closed the run ends on everything the whole run banked.
 */
@Composable
private fun SummaryStage(stage: SessionStage.Summary, onDone: () -> Unit) {
    val finished = stage.run.completed

    if (finished != null) {
        RunCompleteStage(summary = finished, restDaysNow = stage.restDaysNow)
    } else {
        SessionLoggedStage(stage = stage)
    }

    Spacer(Modifier.height(24.dp))
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}

@Composable
private fun RunCompleteStage(summary: RunSummary, restDaysNow: Int) {
    Text(
        "RUN ${summary.runNumber} COMPLETE",
        style = MaterialTheme.typography.headlineMedium,
        color = Palette.Ember
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "${summary.sessions} ${if (summary.sessions == 1) "session" else "sessions"} · " +
            "${summary.days} days",
        style = MaterialTheme.typography.bodyMedium,
        color = Palette.TextSecondary
    )

    Spacer(Modifier.height(20.dp))
    EmberPanel(label = "BANKED") {
        val earned = summary.gains.filter { it.improved }
        if (earned.isEmpty()) {
            Text(
                "No weight added this run. The log still stands and the next run starts " +
                    "from these numbers, not from zero.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.EmberText
            )
        } else {
            earned.forEach { gain ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        gain.exerciseName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${gain.fromKg.kgDisplay()} → ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextSecondary
                    )
                    Text(
                        gain.toKg.kgDisplay(),
                        style = NumeralSmall,
                        color = Palette.Ember
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("+${summary.totalGainKg.kgDisplay()}", style = NumeralLarge, color = Palette.Ember)
            Text(
                "added across this run",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.EmberText
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    SurfacePanel(label = "WHAT CARRIES OVER") {
        Text(
            "Every weight above is where run ${summary.runNumber + 1} starts. Nothing resets.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$restDaysNow", style = NumeralMedium)
            Text(
                " rest days between sessions, up from ${summary.restDaysBefore}",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            summary.plateauedOn?.let {
                "$it stopped beating its own clock — that's the protocol asking for a wider " +
                    "gap, not a session you got wrong."
            } ?: "Run closed. The next block trains on more rest.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary
        )
    }
}

@Composable
private fun SessionLoggedStage(stage: SessionStage.Summary) {
    val evaluation = stage.evaluation

    Text("Session saved", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(4.dp))
    Text(
        "RUN ${stage.run.runNumber} · SESSION ${stage.run.sessionsThisRun}",
        style = MaterialTheme.typography.labelSmall,
        color = Palette.TextSecondary
    )

    Spacer(Modifier.height(20.dp))

    if (evaluation.nextWeights.isNotEmpty()) {
        EmberPanel(label = "YOU JUST EARNED") {
            evaluation.nextWeights.forEach { (name, weight) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(weight.kgDisplay(), style = NumeralSmall, color = Palette.Ember)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Past ${TrainingConstants.TUL_CEILING_SEC}s under load — next session in " +
                    "${stage.restDaysNow} days at these numbers.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.EmberText
            )
        }
    } else {
        SurfacePanel(label = "LOGGED") {
            Text(
                "Nothing crossed ${TrainingConstants.TUL_CEILING_SEC}s, so weights stay where " +
                    "they are. Beat the clock next time, not the bar.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Next session in ${stage.restDaysNow} days.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary
            )
        }
    }
}

@Composable
private fun EmberPanel(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.EmberSurface)
            .border(1.dp, Palette.EmberLine, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.EmberText)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SurfacePanel(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface)
            .padding(18.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.TextTertiary)
        Spacer(Modifier.height(12.dp))
        content()
    }
}
