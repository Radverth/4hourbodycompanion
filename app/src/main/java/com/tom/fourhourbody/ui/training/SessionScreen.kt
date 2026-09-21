package com.tom.fourhourbody.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.tom.fourhourbody.ui.common.NumberField
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.ui.stretches.KeepScreenOn
import com.tom.fourhourbody.ui.stretches.StretchRunner
import com.tom.fourhourbody.ui.theme.NumeralLarge
import com.tom.fourhourbody.ui.theme.NumeralMedium
import com.tom.fourhourbody.ui.theme.NumeralSmall
import com.tom.fourhourbody.ui.theme.Palette
import com.tom.fourhourbody.util.kgDisplay

/**
 * The guided Occam's Protocol session: locked-position cue, glute activation, the strength
 * block at 5s/5s with three minutes of timed rest, then the kettlebell work. Stretches are run
 * through the stretch engine rather than reimplemented here.
 */
@Composable
fun SessionScreen(onExit: () -> Unit) {
    val container = rememberContainer()
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModel.factory(container))
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    val gluteSteps by viewModel.gluteSteps.collectAsStateWithLifecycle()
    val hipFlexorSteps by viewModel.hipFlexorSteps.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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

            SessionStage.LockedPosition -> LockedPositionStage(
                onContinue = viewModel::acknowledgeLockedPosition
            )

            SessionStage.GluteActivation -> {
                Text("Pre-workout glute activation", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Before the first exercise, every session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StretchRunner(
                    steps = gluteSteps,
                    onFinished = { completions ->
                        viewModel.onStretchesDone(completions, SessionStage.Strength(0))
                    },
                    onExit = {
                        viewModel.onStretchesDone(emptyList(), SessionStage.Strength(0))
                    }
                )
            }

            is SessionStage.Strength -> {
                val currentPrompt = prompt
                if (currentPrompt == null) {
                    Text("Loading exercise…")
                } else {
                    StrengthStage(
                        prompt = currentPrompt,
                        onLog = { weight, reps ->
                            viewModel.logExercise(current.index, weight, reps)
                        }
                    )
                }
            }

            is SessionStage.Rest -> {
                Text("Rest", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Exactly ${TrainingConstants.REST_BETWEEN_EXERCISES_SEC / 60} minutes — " +
                        "timed, not eyeballed.",
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
                    "Skipping the rest logs the rest you actually took.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is SessionStage.Stalled -> StalledStage(
                exerciseName = current.exerciseName,
                runNumber = current.runNumber,
                onEndSession = { viewModel.finishSession() }
            )

            SessionStage.KettlebellPrep -> {
                Text("Hip flexor stretch", style = MaterialTheme.typography.titleLarge)
                Text(
                    "The static exception before swings — non-dominant side first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StretchRunner(
                    steps = hipFlexorSteps,
                    onFinished = { completions ->
                        viewModel.onStretchesDone(completions, SessionStage.Tabata)
                    },
                    onExit = { viewModel.onStretchesDone(emptyList(), SessionStage.Tabata) }
                )
            }

            SessionStage.Tabata -> TabataStage(
                defaultBellWeightKg = settings.defaultBellWeightKg,
                onRoundLogged = viewModel::logKettlebellRound,
                onFinished = viewModel::onTabataFinished
            )

            SessionStage.Abs -> AbsStage(onFinished = viewModel::onAbsFinished)

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
private fun LockedPositionStage(onContinue: (Boolean) -> Unit) {
    var dontShowAgain by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Locked position", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(TrainingConstants.LOCKED_POSITION_CUE, style = MaterialTheme.typography.bodyMedium)
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
                Text("Start warm-up")
            }
        }
    }
}

@Composable
private fun StrengthStage(prompt: ExercisePrompt, onLog: (Double, Int) -> Unit) {
    var weightText by remember(prompt.config.id) {
        mutableStateOf(prompt.suggestedWeightKg?.let { "%.1f".format(it) } ?: "")
    }
    var reps by remember(prompt.config.id) { mutableIntStateOf(0) }

    Text(
        "Exercise ${prompt.position} of ${prompt.total}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(prompt.config.exerciseName, style = MaterialTheme.typography.headlineSmall)
    Text(
        "Target: ${prompt.config.targetReps}+ reps to failure · ${prompt.config.equipment}",
        style = MaterialTheme.typography.bodyMedium
    )

    if (prompt.lastWeightKg != null && prompt.lastReps != null) {
        Text(
            "Last time: ${prompt.lastWeightKg.kgDisplay()} × ${prompt.lastReps} reps",
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

    Spacer(Modifier.height(16.dp))
    NumberField(
        label = "Weight (kg)",
        value = weightText,
        onValueChange = { weightText = it },
        decimal = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(16.dp))
    TempoGuide(reps = reps, onRepsChange = { reps = it })

    Spacer(Modifier.height(16.dp))
    Text("Reps: $reps", style = MaterialTheme.typography.headlineSmall)

    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { onLog(weightText.toDoubleOrNull() ?: 0.0, reps) },
        enabled = weightText.toDoubleOrNull() != null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Log set")
    }
}

/**
 * The stall. Worded as the end of a block rather than a failure, because that is what it is:
 * the protocol's own signal that the gap between sessions is now too short.
 */
@Composable
private fun StalledStage(exerciseName: String, runNumber: Int, onEndSession: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Stalled on $exerciseName", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "More than one rep short of target. That closes run $runNumber — the " +
                    "remaining exercises aren't run, every weight you reached is kept, and " +
                    "the next run trains on one more rest day.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onEndSession, modifier = Modifier.fillMaxWidth()) {
                Text("Close run $runNumber")
            }
        }
    }
}

@Composable
private fun TabataStage(
    defaultBellWeightKg: Double,
    onRoundLogged: (Int, Int, Double) -> Unit,
    onFinished: () -> Unit
) {
    var round by remember { mutableIntStateOf(1) }
    var working by remember { mutableStateOf(true) }
    var swings by remember { mutableIntStateOf(0) }

    Text("Kettlebell Tabata", style = MaterialTheme.typography.titleLarge)
    Text(
        "Round $round of ${TrainingConstants.TABATA_ROUNDS} · " +
            "${TrainingConstants.TABATA_WORK_SEC}s work / ${TrainingConstants.TABATA_REST_SEC}s rest",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (working) {
        CountdownBlock(
            totalSec = TrainingConstants.TABATA_WORK_SEC,
            label = "swing",
            autoStart = true,
            secondary = "Swing",
            onFinished = { working = false }
        )
    } else {
        CountdownBlock(
            totalSec = TrainingConstants.TABATA_REST_SEC,
            label = "rest",
            autoStart = true,
            secondary = "Rest — log the round",
            onFinished = { _ ->
                onRoundLogged(round, swings, defaultBellWeightKg)
                if (round >= TrainingConstants.TABATA_ROUNDS) {
                    onFinished()
                } else {
                    round += 1
                    working = true
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { if (swings > 0) swings -= 1 }) { Text("−1") }
            Text("$swings swings", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { swings += 1 }) { Text("+1") }
        }
        Text(
            "The count carries into the next round, so a steady pace needs no retyping.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(Modifier.height(16.dp))
    OutlinedButton(onClick = onFinished) { Text("End kettlebell block") }
}

@Composable
private fun AbsStage(onFinished: () -> Unit) {
    Text("Six-Minute Abs", style = MaterialTheme.typography.titleLarge)
    Text(
        "Optional block: myotatic crunch, then cat vomit.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    CountdownBlock(
        totalSec = TrainingConstants.SIX_MINUTE_ABS_SEC,
        label = "abs block",
        autoStart = false,
        onFinished = { onFinished() }
    )
}

/**
 * How a session ends is what is remembered of it, so it ends on the reward rather than on a
 * receipt. A session inside a run ends on the weights it just earned for next time; a session
 * whose stall closed the run ends on everything the whole run banked.
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
            summary.stalledOn?.let {
                "The stall on $it is the protocol asking for a wider gap — it is how the " +
                    "block is meant to end, not a session you got wrong."
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
                "Next session in ${stage.restDaysNow} days. These are the numbers waiting " +
                    "for you.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.EmberText
            )
        }
    } else {
        SurfacePanel(label = "LOGGED") {
            Text(
                "Targets weren't all hit, so weights stay where they are. The run continues " +
                    "on the same numbers.",
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
