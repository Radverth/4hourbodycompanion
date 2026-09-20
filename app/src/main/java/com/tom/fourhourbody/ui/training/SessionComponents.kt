package com.tom.fourhourbody.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tom.fourhourbody.domain.training.TrainingConstants
import com.tom.fourhourbody.ui.common.BigReadout
import com.tom.fourhourbody.util.Feedback
import com.tom.fourhourbody.util.MonotonicTimer
import com.tom.fourhourbody.util.msAsClock

/**
 * A plain countdown driven by the monotonic clock, so it stays accurate with the screen off.
 * Used for the three-minute rest, the Tabata intervals and the abs block.
 */
@Composable
fun CountdownBlock(
    totalSec: Int,
    label: String,
    autoStart: Boolean,
    onFinished: (elapsedSec: Int) -> Unit,
    modifier: Modifier = Modifier,
    toneOnFinish: Boolean = true,
    secondary: String? = null,
    controls: Boolean = true
) {
    val context = LocalContext.current
    val totalMs = totalSec * 1000L

    var running by remember(totalSec, label) { mutableStateOf(autoStart) }
    var remainingMs by remember(totalSec, label) { mutableLongStateOf(totalMs) }

    LaunchedEffect(running, totalSec, label) {
        if (!running) return@LaunchedEffect
        MonotonicTimer.countdownFlow(remainingMs).collect { remainingMs = it }
        running = false
        if (toneOnFinish) Feedback.completionTone(context)
        onFinished(totalSec)
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BigReadout(primary = remainingMs.msAsClock(), secondary = secondary ?: label)
        LinearProgressIndicator(
            progress = {
                if (totalMs <= 0L) 1f
                else ((totalMs - remainingMs).toFloat() / totalMs).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (controls) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { running = !running }) {
                    Text(if (running) "Pause" else "Start")
                }
                OutlinedButton(
                    onClick = {
                        running = false
                        onFinished(((totalMs - remainingMs) / 1000L).toInt())
                    }
                ) { Text("Skip") }
            }
        }
    }
}

/**
 * The 5 seconds up / 5 seconds down tempo guide. Each completed ten-second cycle counts a rep,
 * and the count stays editable because a rep you had to grind out is still a rep.
 */
@Composable
fun TempoGuide(
    reps: Int,
    onRepsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cycleMs = (TrainingConstants.TEMPO_UP_SEC + TrainingConstants.TEMPO_DOWN_SEC) * 1000L
    val upMs = TrainingConstants.TEMPO_UP_SEC * 1000L

    var running by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var cyclesCounted by remember { mutableIntStateOf(0) }

    // The callback is captured through rememberUpdatedState so the ticking coroutine always
    // increments the current rep count rather than the one it started with.
    val countRep by rememberUpdatedState { onRepsChange(reps + 1) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val base = elapsedMs
        MonotonicTimer.elapsedFlow().collect { tick ->
            val value = base + tick
            elapsedMs = value
            val completed = (value / cycleMs).toInt()
            if (completed > cyclesCounted) {
                cyclesCounted = completed
                countRep()
                Feedback.transitionTone(context)
            }
        }
    }

    val withinCycle = elapsedMs % cycleMs
    val goingUp = withinCycle < upMs
    val phaseProgress = if (goingUp) {
        withinCycle.toFloat() / upMs
    } else {
        (withinCycle - upMs).toFloat() / (cycleMs - upMs)
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (running) (if (goingUp) "UP" else "DOWN") else "Tempo paused",
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { phaseProgress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${TrainingConstants.TEMPO_UP_SEC}s up / ${TrainingConstants.TEMPO_DOWN_SEC}s down",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { running = !running }) { Text(if (running) "Pause" else "Start") }
            OutlinedButton(onClick = { if (reps > 0) onRepsChange(reps - 1) }) { Text("−1 rep") }
            OutlinedButton(onClick = { onRepsChange(reps + 1) }) { Text("+1 rep") }
        }
    }
}
