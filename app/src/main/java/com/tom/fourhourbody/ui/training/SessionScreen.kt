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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.data.entity.PlateauTechnique
import com.tom.fourhourbody.data.entity.SessionKind
import com.tom.fourhourbody.domain.run.RunSummary
import com.tom.fourhourbody.domain.training.NextStep
import com.tom.fourhourbody.domain.training.Plateau
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
 * The guided Body by Science session: locked-position cue, glute activation, then one set per
 * exercise taken to failure at a 10s/10s cadence with 45 seconds between them.
 *
 * Every set is timed rather than counted, and the clock runs upward — see [TulClock]. The
 * stall rule stops the session on the spot, so there is no path from a stalled set back into
 * the remaining exercises.
 */
@Composable
fun SessionScreen(
    onExit: () -> Unit,
    requestedKind: SessionKind = SessionKind.STANDARD
) {
    val container = rememberContainer()
    val viewModel: SessionViewModel = viewModel(factory = SessionViewModel.factory(container))
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    val gluteSteps by viewModel.gluteSteps.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val kind by viewModel.kind.collectAsStateWithLifecycle()

    // The session row is written on start, so starting is a side effect of arriving here and
    // has to happen exactly once rather than on every recomposition.
    LaunchedEffect(requestedKind) { viewModel.start(requestedKind) }

    KeepScreenOn()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (kind != SessionKind.STANDARD && stage !is SessionStage.Summary) {
            Text(
                kindBanner(kind),
                style = MaterialTheme.typography.labelSmall,
                color = Palette.Ember
            )
            Spacer(Modifier.height(10.dp))
        }

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
                        upSec = settings.tempoUpSec,
                        downSec = settings.tempoDownSec,
                        onLog = { weight, position, tul, reps ->
                            viewModel.logExercise(current.index, weight, position, tul, reps)
                        }
                    )
                }
            }

            is SessionStage.Rest -> {
                Text("Rest", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${TrainingConstants.REST_BETWEEN_EXERCISES_SEC} seconds, and not a gate " +
                        "— go when you're ready. The point is to stay huffing and puffing, " +
                        "not to wait out a timer.",
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
                    "Going early logs the rest you actually took.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is SessionStage.Stalled -> StalledStage(
                stage = current,
                onTechniqueLogged = viewModel::logPlateauTechnique,
                onEndSession = { viewModel.finishSession() }
            )

            is SessionStage.Summary -> SummaryStage(stage = current, onDone = onExit)
        }

        if (stage !is SessionStage.Summary) {
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onExit) { Text("Leave session") }
        }
    }
}

private fun kindBanner(kind: SessionKind): String = when (kind) {
    SessionKind.CUTTING -> "CUTTING PHASE · REDUCED VOLUME ON PURPOSE"
    SessionKind.NO_EQUIPMENT -> "NO EQUIPMENT · BOARD AND BODYWEIGHT"
    SessionKind.STANDARD -> ""
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
private fun StrengthStage(
    prompt: ExercisePrompt,
    upSec: Int,
    downSec: Int,
    onLog: (Double, String?, Int, Int) -> Unit
) {
    var weightText by remember(prompt.config.id) {
        mutableStateOf(prompt.suggestedLoad?.weightKg?.let { "%.1f".format(it) } ?: "")
    }
    var positionText by remember(prompt.config.id) {
        mutableStateOf(prompt.suggestedLoad?.position ?: "")
    }

    Text(
        "Exercise ${prompt.position} of ${prompt.total}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(prompt.config.exerciseName, style = MaterialTheme.typography.headlineSmall)
    Text(
        "One set to failure · ${prompt.config.targetTulMinSec}–" +
            "${prompt.config.targetTulMaxSec}s · ${prompt.config.equipment}",
        style = MaterialTheme.typography.bodyMedium
    )
    prompt.config.freeWeightEquivalent?.let {
        Text(
            "No machine? $it",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    prompt.previous?.let { last ->
        Text(
            lastTimeLine(last.load.weightKg, last.load.position, last.tulSeconds, prompt.isBodyweight),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(Modifier.height(16.dp))

    // Bodyweight work has no plates, so the field that matters is the position. Showing a
    // weight box for a wall sit would invite a number that means nothing.
    if (!prompt.isBodyweight) {
        NumberField(
            label = "Weight (kg)",
            value = weightText,
            onValueChange = { weightText = it },
            decimal = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
    }

    OutlinedTextField(
        value = positionText,
        onValueChange = { positionText = it },
        label = { Text(if (prompt.isBodyweight) "Handle position" else "Seat / pin position") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(6.dp))
    Text(
        TrainingConstants.SEAT_POSITION_CUE,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(20.dp))

    val weight = if (prompt.isBodyweight) 0.0 else weightText.toDoubleOrNull()
    if (weight == null) {
        Text(
            "Enter the weight to start the clock.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        // Keyed on the exercise so the clock cannot carry a previous set's seconds into this
        // one. Without it the state would only reset because the rest stage happens to
        // dispose this subtree first.
        key(prompt.config.id) {
            TulClock(
                upSec = upSec,
                downSec = downSec,
                targetMinSec = prompt.config.targetTulMinSec,
                targetMaxSec = prompt.config.targetTulMaxSec,
                onFailure = { tul, reps ->
                    onLog(weight, positionText.takeIf { it.isNotBlank() }, tul, reps)
                }
            )
        }
    }
}

private fun lastTimeLine(
    weightKg: Double,
    position: String?,
    tulSeconds: Int,
    isBodyweight: Boolean
): String {
    val load = if (isBodyweight) position ?: "same position" else weightKg.kgDisplay()
    val suffix = if (!isBodyweight && position != null) " (pos $position)" else ""
    return "Last time: $load$suffix held ${tulSeconds}s"
}

/**
 * The stall, worded as the end of a block rather than a failure, because that is what it is:
 * the protocol's own signal that the gap between sessions is now too short.
 *
 * The plateau toolkit appears here only on a second consecutive stall on the same exercise,
 * and only as an offer. The book is explicit that leaning on these techniques costs more
 * recovery than the plateau they break, so putting them one tap from every stall would be
 * the app pushing the thing its source says to use sparingly.
 */
@Composable
private fun StalledStage(
    stage: SessionStage.Stalled,
    onTechniqueLogged: (String, PlateauTechnique) -> Unit,
    onEndSession: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Stalled on ${stage.exerciseName}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "${stage.tulSeconds}s against ${stage.previousTulSeconds}s at the same load. " +
                    "That closes run ${stage.runNumber} — the remaining exercises aren't run, " +
                    "every load you reached is kept, and the next run trains on one more rest " +
                    "day.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onEndSession, modifier = Modifier.fillMaxWidth()) {
                Text("Close run ${stage.runNumber}")
            }
        }
    }

    stage.plateau?.let { plateau ->
        Spacer(Modifier.height(16.dp))
        PlateauOffer(
            plateau = plateau,
            onTechniqueLogged = { onTechniqueLogged(plateau.exerciseName, it) }
        )
    }
}

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
                "No load added this run. The log still stands and the next run starts from " +
                    "these numbers, not from zero.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.EmberText
            )
        } else {
            earned.forEach { gain ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(gain.exerciseName, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        if (gain.gainedKg > 0.01) {
                            "${gain.fromKg.kgDisplay()} → "
                        } else {
                            "${gain.fromTulSec}s → "
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextSecondary
                    )
                    Text(
                        if (gain.gainedKg > 0.01) gain.toKg.kgDisplay() else "${gain.toTulSec}s",
                        style = NumeralSmall,
                        color = Palette.Ember
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (summary.totalGainKg > 0.01) {
                Text("+${summary.totalGainKg.kgDisplay()}", style = NumeralLarge, color = Palette.Ember)
                Text(
                    "added across this run",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.EmberText
                )
            } else {
                Text("+${summary.totalGainTulSec}s", style = NumeralLarge, color = Palette.Ember)
                Text(
                    "longer under load across this run",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.EmberText
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    SurfacePanel(label = "WHAT CARRIES OVER") {
        Text(
            "Every load above is where run ${summary.runNumber + 1} starts. Nothing resets.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$restDaysNow", style = NumeralMedium)
            Column(Modifier.weight(1f).padding(start = 6.dp, bottom = 3.dp)) {
                Text(
                    "rest days between sessions, up from ${summary.restDaysBefore}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            summary.stalledOn?.let {
                "A set that can't match its own last time is the protocol asking for a wider " +
                    "gap. The stronger you get, the longer a set to failure takes to recover " +
                    "from — the schedule stretching out is that working, not you slipping."
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

    val earned = evaluation.steps.filterValues { it is NextStep.Heavier || it is NextStep.NextPosition }
    if (earned.isNotEmpty()) {
        EmberPanel(label = "YOU JUST EARNED") {
            earned.forEach { (name, step) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(stepValue(step), style = NumeralSmall, color = Palette.Ember)
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
                "Every set landed inside the window, so the loads stay where they are. That " +
                    "is where the protocol wants them — the increase comes when a set runs " +
                    "past the top of the window, not for turning up.",
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

    val belowWindow = evaluation.steps.filterValues { it is NextStep.BelowWindow }
    if (belowWindow.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        SurfacePanel(label = "UNDER THE WINDOW") {
            belowWindow.forEach { (name, step) ->
                val seconds = (step as NextStep.BelowWindow).tulSeconds
                Text("$name — ${seconds}s", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Short of ${TrainingConstants.TARGET_TUL_MIN_SEC}s means the load is heavier " +
                    "than the protocol asks for. It is left to you rather than adjusted " +
                    "automatically — the book specifies when to add load and says nothing " +
                    "about taking it off, and this app shouldn't invent training advice.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    SurfacePanel(label = "TIME") {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${evaluation.totalTulSeconds}s", style = NumeralMedium)
            Column(Modifier.weight(1f).padding(start = 6.dp, bottom = 3.dp)) {
                Text(
                    "under load, across ${stage.elapsedSessionTimeSec / 60} min in the gym",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Both numbers are kept so the second can be checked against the first: if the " +
                "session gets longer while the time under load doesn't, the rests have " +
                "quietly stretched.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary
        )
    }
}

private fun stepValue(step: NextStep): String = when (step) {
    is NextStep.Heavier ->
        if (step.highKg > step.lowKg + 0.01) {
            "${step.lowKg.kgDisplay()}–${step.highKg.kgDisplay()}"
        } else {
            step.lowKg.kgDisplay()
        }
    is NextStep.NextPosition -> step.to
    is NextStep.BelowWindow -> "${step.tulSeconds}s"
    NextStep.Hold -> "—"
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

/** Shared with the standalone plateau screen so the two cannot describe the techniques differently. */
@Composable
internal fun PlateauOffer(
    plateau: Plateau,
    onTechniqueLogged: (PlateauTechnique) -> Unit
) {
    var logged by remember { mutableStateOf<PlateauTechnique?>(null) }

    SurfacePanel(label = "PLATEAU TOOLKIT") {
        Text(
            "${plateau.exerciseName} has stalled ${plateau.consecutiveStalls} sessions " +
                "running, so the extra rest hasn't resolved it on its own.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "These are for exactly this and nothing else. Used routinely they cost more " +
                "recovery than the plateau they're meant to break, which is why nothing here " +
                "is suggested and logging one is optional.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary
        )
        Spacer(Modifier.height(14.dp))
        PlateauTechnique.entries.forEach { technique ->
            Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                Text(technique.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    technique.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        logged = technique
                        onTechniqueLogged(technique)
                    },
                    enabled = logged != technique
                ) {
                    Text(if (logged == technique) "Logged" else "I used this")
                }
            }
        }
        if (logged != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Logged. Nothing changes in the protocol because of it — the record is so " +
                    "repeated use is at least visible.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary
            )
        }
    }
}
