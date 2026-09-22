package com.tom.fourhourbody.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.data.repo.DashboardState
import com.tom.fourhourbody.data.repo.RunStatus
import com.tom.fourhourbody.domain.today.Focus
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.ui.nav.Routes
import com.tom.fourhourbody.ui.theme.CalloutShape
import com.tom.fourhourbody.ui.theme.ForgeButtonShape
import com.tom.fourhourbody.ui.theme.Palette
import com.tom.fourhourbody.ui.theme.RunicLabel
import com.tom.fourhourbody.util.displayLong
import com.tom.fourhourbody.util.kgDisplay

/**
 * Today, and one question: what now.
 *
 * With a single pillar, "what now" only ever has one real answer — a session due or not — so
 * this screen is a hero card and a standing line underneath it, nothing more. Review — runs,
 * milestones, the exercise loadout — lives on the deck; this screen is only ever acted on.
 */
@Composable
fun DashboardScreen(onOpenPillar: (String) -> Unit) {
    val container = rememberContainer()
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val nextWeights by viewModel.nextWeights.collectAsStateWithLifecycle()
    val runStatus by viewModel.runStatus.collectAsStateWithLifecycle()
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
            trainingIntention = settings.trainingIntention,
            nextWeights = nextWeights,
            onOpenPillar = onOpenPillar
        )

        Spacer(Modifier.height(30.dp))
        StandingLine(runStatus, level)
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
    trainingIntention: String?,
    nextWeights: List<NextWeight>,
    onOpenPillar: (String) -> Unit
) {
    when (focus) {
        Focus.TRAIN -> HeroCard(
            kicker = "SESSION DUE",
            headline = trainingIntention?.let { "You said you'd $it." } ?: "Train today.",
            body = nextWeights.take(2).joinToString(", ") {
                "${it.exerciseName.lowercase()} ${it.weightKg.kgDisplay()}"
            }.takeIf { it.isNotBlank() }?.let { "Waiting for you: $it." },
            action = "Start session",
            onAction = { onOpenPillar(Routes.SESSION) }
        )

        Focus.CLEAR -> ClearCard(state, nextWeights)
    }
}

@Composable
private fun HeroCard(
    kicker: String,
    headline: String,
    body: String?,
    action: String?,
    onAction: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CalloutShape)
            .background(Palette.EmberSurface)
            .border(1.dp, Palette.EmberLine, CalloutShape)
            .padding(20.dp)
    ) {
        // Today borrows the character sheet's voice and nothing else: the typeface and the
        // bevel, not the brass frames. One screen is inspected, this one is acted on.
        Text(kicker, style = RunicLabel, color = Palette.EmberText)
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
                shape = ForgeButtonShape,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(action.uppercase(), style = RunicLabel)
            }
        }
    }
}

/**
 * Nothing is asking. The screen says so plainly rather than inventing a task — a tracker that
 * always has something for you is one you stop believing.
 */
@Composable
private fun ClearCard(state: DashboardState, nextWeights: List<NextWeight>) {
    val daysUntil = state.training.nextSessionDate
        ?.let { java.time.temporal.ChronoUnit.DAYS.between(state.date, it) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface)
            .padding(20.dp)
    ) {
        Text("NOTHING LEFT", style = RunicLabel, color = Palette.TextTertiary)
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
                else -> "Nothing scheduled."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextSecondary
        )
    }
}

// ---------------------------------------------------------------------------------------
// Standing: what level, run and session are, in one line that never competes with the
// thing to do.
// ---------------------------------------------------------------------------------------

@Composable
private fun StandingLine(runStatus: RunStatus?, level: Int) {
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
        },
        style = MaterialTheme.typography.bodySmall,
        color = Palette.TextTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
