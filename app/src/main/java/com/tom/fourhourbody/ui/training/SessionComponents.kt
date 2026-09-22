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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tom.fourhourbody.domain.training.TrainingConstants
import com.tom.fourhourbody.ui.common.BigReadout
import com.tom.fourhourbody.util.Feedback
import com.tom.fourhourbody.util.MonotonicTimer
import com.tom.fourhourbody.util.msAsClock

/**
 * A plain countdown driven by the monotonic clock, so it stays accurate with the screen off.
 * Used for the brisk transition between exercises.
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
 * The working set. A stopwatch, not a rep counter — time under load is the book's real measure
 * of a set, and the book is explicit that a slower cadence near failure is expected, not a
 * fault, so counting cycles would misrepresent the set it's timing.
 *
 * The cadence guide underneath is a pacing aid only: an audible cue on every up/down reversal,
 * on the book's own worked example of 10 seconds up / 10 seconds down. Nothing reads it back —
 * "as slow as you can without stopping" is a feel, not a number this app can verify.
 */
@Composable
fun WorkingSetTimer(
    onFailure: (tulSec: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cycleMs = (TrainingConstants.TEMPO_UP_SEC + TrainingConstants.TEMPO_DOWN_SEC) * 1000L
    val upMs = TrainingConstants.TEMPO_UP_SEC * 1000L

    var running by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var wasGoingUp by remember { mutableStateOf(true) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        MonotonicTimer.elapsedFlow().collect { tick ->
            elapsedMs = tick
            val nowGoingUp = (tick % cycleMs) < upMs
            if (wasGoingUp != nowGoingUp) {
                Feedback.turnTone(context)
                wasGoingUp = nowGoingUp
            }
        }
    }

    val goingUp = (elapsedMs % cycleMs) < upMs

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BigReadout(
            primary = elapsedMs.msAsClock(),
            secondary = when {
                !running -> "Ready"
                goingUp -> "UP"
                else -> "DOWN"
            }
        )
        Text(
            "Cadence guide: ${TrainingConstants.TEMPO_UP_SEC}s up / " +
                "${TrainingConstants.TEMPO_DOWN_SEC}s down — as slow as you can manage without " +
                "stopping and starting. Target: under ${TrainingConstants.TUL_CEILING_SEC}s.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        if (!running) {
            Button(onClick = { running = true; elapsedMs = 0L }, modifier = Modifier.fillMaxWidth()) {
                Text("Start set")
            }
        } else {
            Button(
                onClick = {
                    running = false
                    Feedback.completionTone(context)
                    onFailure((((elapsedMs + 999) / 1000)).toInt())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reached failure")
            }
        }
    }
}
