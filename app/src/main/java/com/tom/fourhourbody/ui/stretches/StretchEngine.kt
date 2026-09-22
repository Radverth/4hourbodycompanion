package com.tom.fourhourbody.ui.stretches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import com.tom.fourhourbody.ui.theme.Palette
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.tom.fourhourbody.data.entity.StretchConfigEntity
import com.tom.fourhourbody.data.entity.StretchMode
import com.tom.fourhourbody.data.entity.StretchRoutine
import com.tom.fourhourbody.domain.stretch.StretchGuide
import com.tom.fourhourbody.domain.stretch.StretchGuides
import com.tom.fourhourbody.ui.common.BigReadout
import com.tom.fourhourbody.util.Feedback
import com.tom.fourhourbody.util.MonotonicTimer
import com.tom.fourhourbody.util.msAsClock

/** One thing to do: a single hold, or a single set of reps. */
data class StretchStep(
    val configId: Long,
    val name: String,
    val routine: StretchRoutine,
    val mode: StretchMode,
    val holdSec: Int,
    val reps: Int,
    val sideOrPosition: String,
    val notes: String?
)

/** What actually happened, ready to become a [com.tom.fourhourbody.data.entity.StretchLogEntity]. */
data class StretchCompletion(
    val configId: Long,
    val name: String,
    val routine: StretchRoutine,
    val sideOrPosition: String,
    val durationSec: Int,
    val repsCompleted: Int?
)

/**
 * Expands a config into the steps it actually involves: one per side or position, and one per
 * set for rep-based entries, so each log row is a single thing that was done.
 */
fun StretchConfigEntity.toSteps(): List<StretchStep> {
    val sides = defaultSide
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.ifEmpty { null }
        ?: listOf("")
    val sets = (defaultSets ?: 1).coerceAtLeast(1)

    return sides.flatMap { side ->
        (1..sets).map { setIndex ->
            val label = buildString {
                if (side.isNotEmpty()) append(side)
                if (sets > 1) {
                    if (isNotEmpty()) append(" — ")
                    append("set $setIndex of $sets")
                }
                if (isEmpty()) append("Single")
            }
            StretchStep(
                configId = id,
                name = stretchName,
                routine = routine,
                mode = mode,
                holdSec = defaultHoldSec ?: 0,
                reps = defaultReps ?: 0,
                sideOrPosition = label,
                notes = notes
            )
        }
    }
}

/**
 * The one stretch engine. Hold mode is a calm countdown with a single tone at the end — no
 * per-second beeping. Reps mode is a plain counter, because Active Bridges is a rep exercise
 * and forcing a timer onto it would be wrong.
 */
@Composable
fun StretchRunner(
    steps: List<StretchStep>,
    onFinished: (List<StretchCompletion>) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (steps.isEmpty()) {
        Column(modifier.fillMaxWidth().padding(24.dp)) {
            Text("Nothing to do here — every stretch in this routine is switched off.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onExit) { Text("Back") }
        }
        return
    }

    val completions = remember { mutableListOf<StretchCompletion>() }
    var index by remember { mutableIntStateOf(0) }
    val step = steps[index]

    fun advance(completion: StretchCompletion?) {
        if (completion != null) completions += completion
        if (index == steps.lastIndex) {
            onFinished(completions.toList())
        } else {
            index += 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Step ${index + 1} of ${steps.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(step.name, style = MaterialTheme.typography.headlineSmall)
        Text(step.sideOrPosition, style = MaterialTheme.typography.titleMedium)
        if (step.notes != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                step.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        StretchGuides[step.name]?.let { guide ->
            Spacer(Modifier.height(10.dp))
            HowToPanel(guide)
        }

        when (step.mode) {
            StretchMode.HOLD -> HoldStep(
                step = step,
                onDone = { heldSec ->
                    advance(
                        StretchCompletion(
                            configId = step.configId,
                            name = step.name,
                            routine = step.routine,
                            sideOrPosition = step.sideOrPosition,
                            durationSec = heldSec,
                            repsCompleted = null
                        )
                    )
                }
            )

            StretchMode.REPS -> RepsStep(
                step = step,
                onDone = { reps ->
                    advance(
                        StretchCompletion(
                            configId = step.configId,
                            name = step.name,
                            routine = step.routine,
                            sideOrPosition = step.sideOrPosition,
                            durationSec = 0,
                            repsCompleted = reps
                        )
                    )
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { advance(null) }) { Text("Skip") }
            TextButton(onClick = onExit) { Text("Exit") }
        }
    }
}

@Composable
private fun HoldStep(step: StretchStep, onDone: (Int) -> Unit) {
    val context = LocalContext.current
    val totalMs = step.holdSec * 1000L

    var running by remember(step) { mutableStateOf(false) }
    var remainingMs by remember(step) { mutableLongStateOf(totalMs) }
    var finished by remember(step) { mutableStateOf(false) }

    LaunchedEffect(running, step) {
        if (!running) return@LaunchedEffect
        // Restarting from the paused remainder keeps the countdown monotonic across pauses.
        MonotonicTimer.countdownFlow(remainingMs).collect { remainingMs = it }
        running = false
        finished = true
        Feedback.completionTone(context)
    }

    BigReadout(
        primary = remainingMs.msAsClock(),
        secondary = if (finished) "Hold complete" else "${step.holdSec}s hold"
    )

    LinearProgressIndicator(
        progress = {
            if (totalMs <= 0L) 1f else ((totalMs - remainingMs).toFloat() / totalMs).coerceIn(0f, 1f)
        },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!finished) {
            Button(onClick = { running = !running }) {
                Text(if (running) "Pause" else if (remainingMs == totalMs) "Start" else "Resume")
            }
        }
        OutlinedButton(
            onClick = {
                val held = ((totalMs - remainingMs) / 1000L).toInt()
                onDone(if (finished) step.holdSec else held)
            }
        ) {
            Text(if (finished) "Next" else "Done early")
        }
    }
}

@Composable
private fun RepsStep(step: StretchStep, onDone: (Int) -> Unit) {
    var count by remember(step) { mutableIntStateOf(0) }

    BigReadout(primary = "$count", secondary = "of ${step.reps} reps")

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { if (count > 0) count -= 1 }) { Text("−1") }
        Button(onClick = { count += 1 }) { Text("+1") }
    }

    Spacer(Modifier.height(16.dp))
    Button(onClick = { onDone(count) }, modifier = Modifier.fillMaxWidth()) {
        Text("Done")
    }
}

/** Timer screens are looked at, not tapped — letting the screen sleep mid-hold is unhelpful. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * How to do the stretch, folded away by default.
 *
 * Open while you are learning it, shut once you are not — a timer running under a wall of
 * instructions is worse than either on its own. The state is per-step, so opening it on one
 * stretch does not leave it open on the next.
 */
@Composable
private fun HowToPanel(guide: StretchGuide) {
    var expanded by remember(guide) { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Hide how to do it" else "How to do it")
        }

        if (expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.Surface)
                    .padding(14.dp)
            ) {
                GuideBlock("SET UP", guide.setup)
                Spacer(Modifier.height(10.dp))
                GuideBlock("DO THIS", guide.execution)
                Spacer(Modifier.height(10.dp))
                Text(
                    "WATCH FOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.Warn
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    guide.watchFor,
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun GuideBlock(label: String, lines: List<String>) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.TextTertiary)
    Spacer(Modifier.height(4.dp))
    lines.forEachIndexed { index, line ->
        Row(Modifier.fillMaxWidth().padding(top = if (index == 0) 0.dp else 5.dp)) {
            Text(
                "${index + 1}.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextTertiary,
                modifier = Modifier.width(20.dp)
            )
            Text(
                line,
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary
            )
        }
    }
}
