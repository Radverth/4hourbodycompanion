package com.tom.fourhourbody.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.data.repo.DashboardState
import com.tom.fourhourbody.data.repo.MotivationState
import com.tom.fourhourbody.data.repo.RunStatus
import com.tom.fourhourbody.domain.today.Focus
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.ui.nav.Routes
import com.tom.fourhourbody.ui.theme.Palette
import com.tom.fourhourbody.util.asTimeOfDay
import com.tom.fourhourbody.util.displayLong
import com.tom.fourhourbody.util.kgDisplay

/**
 * Today, and one question: what now.
 *
 * This screen used to carry nine blocks, because one was added every time something new was
 * built and none was ever taken away. Each was defensible alone and together they were a wall
 * you had to read before you could act. What is left is the thing that is due, a mark for
 * every other pillar, and one quiet line of standing. Everything that is review rather than
 * action now lives on the deck.
 */
@Composable
fun DashboardScreen(onOpenPillar: (String) -> Unit) {
    val container = rememberContainer()
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val motivation by viewModel.motivation.collectAsStateWithLifecycle()
    val nextWeights by viewModel.nextWeights.collectAsStateWithLifecycle()
    val runStatus by viewModel.runStatus.collectAsStateWithLifecycle()
    val synergyNudge by viewModel.synergyNudge.collectAsStateWithLifecycle()
    val focus by viewModel.focus.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()

    val current = state

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text(
            current?.date?.displayLong() ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextSecondary
        )

        if (current == null) {
            Spacer(Modifier.height(24.dp))
            Text("Loading…", color = Palette.TextSecondary)
            return@Column
        }

        Spacer(Modifier.height(22.dp))
        Hero(
            focus = focus,
            state = current,
            motivation = motivation,
            nextWeights = nextWeights,
            comboPrompt = synergyNudge?.synergy?.prompt,
            comboName = synergyNudge?.synergy?.name,
            onOpenPillar = onOpenPillar,
            onMarkDayClean = viewModel::markDayClean
        )

        Spacer(Modifier.height(30.dp))
        PillarDots(
            state = current,
            onOpenPillar = onOpenPillar,
            onMarkDayClean = viewModel::markDayClean,
            onTakeCreatine = viewModel::takeNextCreatineDose
        )

        Spacer(Modifier.height(30.dp))
        StandingLine(runStatus, motivation, level)
        Spacer(Modifier.height(20.dp))
    }
}

// ---------------------------------------------------------------------------------------
// The hero: one thing, with what it pays before you do it rather than after.
// ---------------------------------------------------------------------------------------

@Composable
private fun Hero(
    focus: Focus,
    state: DashboardState,
    motivation: MotivationState?,
    nextWeights: List<NextWeight>,
    comboPrompt: String?,
    comboName: String?,
    onOpenPillar: (String) -> Unit,
    onMarkDayClean: () -> Unit
) {
    val onShift = motivation?.onShiftNow == true
    val shiftEnd = motivation?.shiftEndsAtMinutes

    when (focus) {
        Focus.TRAIN -> HeroCard(
            kicker = if (onShift) "SESSION DUE — AFTER WORK" else "SESSION DUE",
            headline = intention(onShift, shiftEnd, motivation?.trainingIntention),
            body = nextWeights.take(2).joinToString(", ") {
                "${it.exerciseName.lowercase()} ${it.weightKg.kgDisplay()}"
            }.takeIf { it.isNotBlank() }?.let { "Waiting for you: $it." },
            action = "Start session",
            onAction = { onOpenPillar(Routes.SESSION) },
            secondary = "Or just the warm-up — 3 minutes",
            onSecondary = { onOpenPillar(Routes.SESSION) }
        )

        Focus.COMBO -> HeroCard(
            kicker = "ONE HALF IN${comboName?.let { " — ${it.uppercase()}" }.orEmpty()}",
            headline = comboPrompt ?: "One step left.",
            body = null,
            action = null,
            onAction = {},
            secondary = null,
            onSecondary = {}
        )

        Focus.LOG_DAY -> HeroCard(
            kicker = "TODAY'S DIET",
            headline = if (state.nutrition.isCheatDay) {
                "Cheat day. Damage control is the only thing asked."
            } else {
                "One tap if all three rules held."
            },
            body = motivation?.nutrition?.current
                ?.takeIf { it > 0 }
                ?.let { "$it-day chain. This is what keeps it." },
            action = if (state.nutrition.isCheatDay) "Open damage control" else "All three held",
            onAction = {
                if (state.nutrition.isCheatDay) onOpenPillar(Routes.NUTRITION) else onMarkDayClean()
            },
            secondary = if (state.nutrition.isCheatDay) null else "Something slipped — open it",
            onSecondary = { onOpenPillar(Routes.NUTRITION) }
        )

        Focus.CLEAR -> ClearCard(state, motivation, nextWeights)
    }
}

/** Your own plan played back, or the shift that has to end first, or the plain ask. */
private fun intention(onShift: Boolean, shiftEndMinutes: Int?, trainingIntention: String?): String =
    when {
        onShift && shiftEndMinutes != null -> "Train after ${shiftEndMinutes.asTimeOfDay()}."
        trainingIntention != null -> "You said you'd $trainingIntention."
        else -> "Train, then you're done for the day."
    }

@Composable
private fun HeroCard(
    kicker: String,
    headline: String,
    body: String?,
    action: String?,
    onAction: () -> Unit,
    secondary: String?,
    onSecondary: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.EmberSurface)
            .border(1.dp, Palette.EmberLine, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Text(kicker, style = MaterialTheme.typography.labelSmall, color = Palette.EmberText)
        Spacer(Modifier.height(8.dp))
        Text(headline, style = MaterialTheme.typography.headlineSmall)

        if (body != null) {
            Spacer(Modifier.height(10.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Palette.EmberText)
        }

        if (action != null) {
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(action, style = MaterialTheme.typography.labelLarge)
            }
        }

        if (secondary != null) {
            TextButton(
                onClick = onSecondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.EmberText
                )
            }
        }
    }
}

/**
 * Nothing is asking. The screen says so plainly rather than inventing a task — a tracker that
 * always has something for you is one you stop believing.
 */
@Composable
private fun ClearCard(
    state: DashboardState,
    motivation: MotivationState?,
    nextWeights: List<NextWeight>
) {
    val daysUntil = state.training.nextSessionDate
        ?.let { java.time.temporal.ChronoUnit.DAYS.between(state.date, it) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface)
            .padding(20.dp)
    ) {
        Text("NOTHING LEFT", style = MaterialTheme.typography.labelSmall, color = Palette.TextTertiary)
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.training.completedToday) "Session logged. Day's done." else "Day's done.",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                daysUntil != null && daysUntil > 0 && nextWeights.isNotEmpty() -> {
                    val lead = nextWeights.first()
                    "Next session in $daysUntil days — ${lead.exerciseName.lowercase()} " +
                        "${lead.weightKg.kgDisplay()} is waiting."
                }
                daysUntil != null && daysUntil > 0 -> "Next session in $daysUntil days."
                motivation != null && motivation.cheatDayIn > 0 ->
                    "${motivation.cheatDayName} in ${motivation.cheatDayIn} days."
                else -> "Nothing scheduled."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextSecondary
        )
    }
}

// ---------------------------------------------------------------------------------------
// The dots: five pillars in one strip, where five rows with five buttons used to be.
// ---------------------------------------------------------------------------------------

private data class PillarDot(
    val pillar: Pillar,
    val label: String,
    val done: Boolean,
    val onTap: () -> Unit
)

@Composable
private fun PillarDots(
    state: DashboardState,
    onOpenPillar: (String) -> Unit,
    onMarkDayClean: () -> Unit,
    onTakeCreatine: () -> Unit
) {
    val settings = state.settings
    val creatine = state.creatine
    val nutritionDone = state.nutrition.dayLogged

    val dots = listOfNotNull(
        PillarDot(
            pillar = Pillar.STRETCHES,
            label = "Stretch",
            done = state.stretches.deskResetCountToday > 0 || state.stretches.inlineDoneThisSession,
            onTap = { onOpenPillar(Routes.deskReset(false)) }
        ).takeIf { settings.isEnabled(Pillar.STRETCHES) },

        PillarDot(
            pillar = Pillar.NUTRITION,
            label = "Food",
            done = nutritionDone,
            // The compliant day costs one tap and never leaves this screen.
            onTap = { if (nutritionDone) onOpenPillar(Routes.NUTRITION) else onMarkDayClean() }
        ).takeIf { settings.isEnabled(Pillar.NUTRITION) },

        PillarDot(
            pillar = Pillar.SLEEP,
            label = "Sleep",
            done = state.sleep.logged,
            onTap = { onOpenPillar(Routes.SLEEP) }
        ).takeIf { settings.isEnabled(Pillar.SLEEP) },

        PillarDot(
            pillar = Pillar.COLD,
            label = "Cold",
            done = state.cold.countThisWeek > 0,
            onTap = { onOpenPillar(Routes.COLD) }
        ).takeIf { settings.isEnabled(Pillar.COLD) },

        PillarDot(
            pillar = Pillar.CREATINE,
            label = "Creatine",
            done = creatine.morningTaken && creatine.eveningTaken,
            // A dose is a yes/no, so it logs in place; only a missing cycle needs the screen.
            onTap = {
                if (creatine.cycleDay == null) onOpenPillar(Routes.CREATINE) else onTakeCreatine()
            }
        ).takeIf { settings.isEnabled(Pillar.CREATINE) }
    )

    if (dots.isEmpty()) return

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        dots.forEach { dot ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = dot.onTap)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Dot(colour = Palette.of(dot.pillar), filled = dot.done)
                Spacer(Modifier.height(8.dp))
                Text(
                    dot.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dot.done) Palette.TextSecondary else Palette.TextTertiary
                )
            }
        }
    }

    val left = dots.count { !it.done }
    Spacer(Modifier.height(6.dp))
    Text(
        when (left) {
            0 -> "All five logged."
            1 -> "One left — tap to log it."
            else -> "$left left — tap to log."
        },
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Dot(colour: Color, filled: Boolean) {
    if (filled) {
        Box(Modifier.size(14.dp).clip(RoundedCornerShape(999.dp)).background(colour))
    } else {
        Box(
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(999.dp))
                .border(1.5.dp, Palette.DotEmpty, RoundedCornerShape(999.dp))
        )
    }
}

// ---------------------------------------------------------------------------------------
// Standing: what the status strip, the chain pill and the adherence header all said,
// in one line that never competes with the thing to do.
// ---------------------------------------------------------------------------------------

@Composable
private fun StandingLine(runStatus: RunStatus?, motivation: MotivationState?, level: Int) {
    val chain = motivation?.headline

    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
    Spacer(Modifier.height(14.dp))
    Text(
        buildString {
            if (level > 0) append("Level $level")
            if (runStatus != null) {
                if (isNotEmpty()) append(" · ")
                append("Run ${runStatus.runNumber}")
                if (runStatus.started && runStatus.sessionsThisRun > 0) {
                    append(" · session ${runStatus.sessionsThisRun}")
                } else if (!runStatus.started) {
                    append(" · not started")
                }
            }
            if (chain != null) {
                if (isNotEmpty()) append(" · ")
                append("${chain.current}-day chain")
                if (chain.current > 0 && chain.passesLeft > 0) append(", ${chain.passesLeft} pass left")
            }
        },
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
