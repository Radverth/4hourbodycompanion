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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tom.fourhourbody.ui.common.BigReadout
import com.tom.fourhourbody.ui.common.ChipRow
import com.tom.fourhourbody.ui.theme.NumeralLarge
import com.tom.fourhourbody.ui.theme.Palette
import com.tom.fourhourbody.util.Feedback
import com.tom.fourhourbody.util.MonotonicTimer
import com.tom.fourhourbody.util.msAsClock

/**
 * A plain countdown driven by the monotonic clock, so it stays accurate with the screen off.
 * Used for the rest between exercises, which is the only countdown left in a session now that
 * the set itself is timed upward.
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
 * The time-under-load clock.
 *
 * This counts up, not down, and that inversion is the protocol. There is no rep target to
 * reach and no interval to survive: you hold the cadence until the muscle will not complete
 * another rep, and the only question afterwards is how long that took. A countdown would be
 * telling you when to stop, which is the one thing the set has to decide for itself.
 *
 * The cadence is announced out loud because at ten seconds a phase you cannot watch a screen
 * and keep form at the same time. Two distinct tones: one at the turn, one on a completed
 * rep. Reps are counted and shown small, because they are a by-product here — at a fixed
 * cadence the count is just the clock divided by twenty, and the moment the cadence drifts it
 * stops meaning even that.
 */
@Composable
fun TulClock(
    upSec: Int,
    downSec: Int,
    targetMinSec: Int,
    targetMaxSec: Int,
    onFailure: (tulSeconds: Int, reps: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cycleMs = ((upSec + downSec).coerceAtLeast(1)) * 1000L
    val upMs = upSec.coerceAtLeast(1) * 1000L

    var running by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var reps by remember { mutableIntStateOf(0) }

    // Which half of the cycle the previous tick was in, so the turn is called the moment it
    // changes rather than on the next whole second. Starts true: a rep begins on the way up.
    var wasGoingUp by remember { mutableStateOf(true) }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val base = elapsedMs
        MonotonicTimer.elapsedFlow().collect { tick ->
            val value = base + tick
            elapsedMs = value
            val completed = (value / cycleMs).toInt()
            if (completed > reps) {
                reps = completed
                Feedback.transitionTone(context)
                wasGoingUp = true
            } else {
                val nowGoingUp = (value % cycleMs) < upMs
                if (wasGoingUp && !nowGoingUp) Feedback.turnTone(context)
                wasGoingUp = nowGoingUp
            }
        }
    }

    val elapsedSec = (elapsedMs / 1000L).toInt()
    val withinCycle = elapsedMs % cycleMs
    val goingUp = withinCycle < upMs
    val phaseMs = if (goingUp) upMs else cycleMs - upMs
    val intoPhase = if (goingUp) withinCycle else withinCycle - upMs

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$elapsedSec", style = NumeralLarge, color = tulColour(elapsedSec, targetMinSec, targetMaxSec))
        Text(
            "seconds under load",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))

        // The bar fills to the top of the window rather than to a finish line, so going past
        // it reads as clearing the window — which is exactly what earns more load.
        LinearProgressIndicator(
            progress = { (elapsedSec.toFloat() / targetMaxSec.coerceAtLeast(1)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            windowLabel(elapsedSec, targetMinSec, targetMaxSec),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))

        Text(
            if (running) (if (goingUp) "UP" else "DOWN") else "Paused",
            style = MaterialTheme.typography.displaySmall
        )
        if (running) {
            val leftSec = ((phaseMs - intoPhase) / 1000L) + 1
            Text(
                "$leftSec",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (intoPhase.toFloat() / phaseMs).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${upSec}s up / ${downSec}s down · $reps ${if (reps == 1) "rep" else "reps"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))
        ChipRow {
            OutlinedButton(onClick = { running = !running }) {
                Text(if (running) "Pause" else if (elapsedMs > 0L) "Resume" else "Start")
            }
            Button(
                onClick = {
                    running = false
                    Feedback.completionTone(context)
                    onFailure(elapsedSec, reps)
                },
                enabled = elapsedMs > 0L
            ) { Text("Reached failure") }
        }
    }
}

@Composable
private fun tulColour(elapsedSec: Int, minSec: Int, maxSec: Int) = when {
    elapsedSec > maxSec -> Palette.Gain
    elapsedSec >= minSec -> Palette.Ember
    else -> MaterialTheme.colorScheme.onSurface
}

private fun windowLabel(elapsedSec: Int, minSec: Int, maxSec: Int): String = when {
    elapsedSec > maxSec -> "past $maxSec\u2009s — this earns more load next session"
    elapsedSec >= minSec -> "inside the $minSec–$maxSec\u2009s window"
    else -> "target $minSec–$maxSec\u2009s"
}
