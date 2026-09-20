package com.tom.fourhourbody.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.domain.training.TrainingConstants
import com.tom.fourhourbody.ui.common.CheckRow
import com.tom.fourhourbody.ui.common.NumberField
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.ui.stretches.KeepScreenOn
import com.tom.fourhourbody.ui.stretches.StretchRunner
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

@Composable
private fun StalledStage(exerciseName: String, onEndSession: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Stalled on $exerciseName", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "More than one rep short of target. The session ends here — the remaining " +
                    "exercises aren't run — and the gap before the next session grows by one " +
                    "rest day.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onEndSession, modifier = Modifier.fillMaxWidth()) {
                Text("End session")
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

@Composable
private fun SummaryStage(stage: SessionStage.Summary, onDone: () -> Unit) {
    val evaluation = stage.evaluation

    Text("Session saved", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))

    if (evaluation.stalled) {
        Text(
            "Stalled on ${evaluation.stalledOn}. Rest days between sessions are now " +
                "${stage.restDaysNow} — the next session is scheduled from that gap, not a " +
                "fixed weekday.",
            style = MaterialTheme.typography.bodyMedium
        )
    } else if (evaluation.nextWeights.isNotEmpty()) {
        Text("Every target hit. Next session:", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        evaluation.nextWeights.forEach { (name, weight) ->
            Text("$name → ${weight.kgDisplay()}", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        Text(
            "Logged. Targets weren't all hit, so weights stay where they are.",
            style = MaterialTheme.typography.bodyMedium
        )
    }

    Spacer(Modifier.height(8.dp))
    Text(
        "Rest days between sessions: ${stage.restDaysNow}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(24.dp))
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}
