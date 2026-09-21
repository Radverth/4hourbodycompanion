package com.tom.fourhourbody.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.data.entity.Pillar
import com.tom.fourhourbody.data.repo.DashboardState
import com.tom.fourhourbody.data.repo.MotivationState
import com.tom.fourhourbody.domain.creatine.CreatineCycle
import com.tom.fourhourbody.ui.common.ActionRow
import com.tom.fourhourbody.ui.common.ChainPill
import com.tom.fourhourbody.ui.common.CompletionRing
import com.tom.fourhourbody.ui.common.LabelledProgress
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.ui.nav.Routes
import com.tom.fourhourbody.ui.theme.NumeralSmall
import com.tom.fourhourbody.ui.theme.Palette
import com.tom.fourhourbody.util.displayShort
import com.tom.fourhourbody.util.kgDisplay

/**
 * Today. Not a status report — the one thing that matters gets the colour, the size and the
 * only primary button, and every other pillar carries its action inline so nothing has to be
 * navigated to first.
 */
@Composable
fun DashboardScreen(onOpenPillar: (String) -> Unit) {
    val container = rememberContainer()
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val motivation by viewModel.motivation.collectAsStateWithLifecycle()
    val nextWeights by viewModel.nextWeights.collectAsStateWithLifecycle()
    val adherence by viewModel.adherence.collectAsStateWithLifecycle()
    val window by viewModel.window.collectAsStateWithLifecycle()

    val current = state

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Today", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        current?.date?.displayShort() ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.TextSecondary
                    )
                }
                motivation?.let { m ->
                    if (m.headline.current > 0) {
                        ChainPill(days = m.headline.current, passesLeft = m.headline.passesLeft)
                    }
                }
            }
        }

        if (current == null) {
            item { Text("Loading…", Modifier.padding(20.dp)) }
            return@LazyColumn
        }

        item { IntentionStrip(current, motivation) }

        if (current.settings.isEnabled(Pillar.TRAINING)) {
            item { TrainingHero(current, nextWeights, onOpenPillar) }
        }

        if (current.settings.isEnabled(Pillar.NUTRITION)) {
            motivation?.let { m -> item { CheatDayStrip(m) } }
        }

        item { Spacer(Modifier.height(12.dp)) }

        pillarRows(current, motivation, viewModel::markDayClean, onOpenPillar)

        item {
            Spacer(Modifier.height(20.dp))
            Text(
                "Adherence",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(7, 30).forEach { days ->
                    FilterChip(
                        selected = window == days,
                        onClick = { viewModel.setWindow(days) },
                        label = { Text("Last $days days") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Palette.Ember,
                            selectedLabelColor = Palette.EmberInk
                        )
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        items(adherence, key = { it.pillar.name }) { entry ->
            LabelledProgress(
                label = entry.pillar.label,
                detail = entry.detail,
                percent = entry.percent,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

/**
 * Your own plan, played back at the moment of decision. Stating when and where is one of the
 * few interventions with a large, repeatedly replicated effect on follow-through.
 */
@Composable
private fun IntentionStrip(state: DashboardState, motivation: MotivationState?) {
    val enabled = state.settings.enabledPillars.size
    val done = listOf(
        state.training.completedToday,
        state.stretches.deskResetCountToday > 0 || state.stretches.inlineDoneThisSession,
        state.nutrition.dayLogged,
        state.sleep.logged,
        state.creatine.morningTaken && state.creatine.eveningTaken
    ).count { it }

    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        CompletionRing(done = done, total = enabled.coerceAtLeast(1))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            val intention = motivation?.trainingIntention
            Text(
                if (intention != null) "You said you'd $intention." else "$done of $enabled done today.",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (intention != null) {
                    "$done of $enabled done today."
                } else {
                    "Set when you'll train in Settings and this will remind you."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary
            )
        }
    }
}

@Composable
private fun TrainingHero(
    state: DashboardState,
    nextWeights: List<NextWeight>,
    onOpenPillar: (String) -> Unit
) {
    val training = state.training
    val due = training.dueToday && !training.completedToday

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (due) Palette.EmberSurface else Palette.Surface)
            .then(
                if (due) {
                    Modifier.border(1.dp, Palette.EmberLine, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            )
            .padding(18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (due) {
                Text(
                    "SESSION DUE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.EmberInk,
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Palette.Ember)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else {
                Text(
                    if (training.completedToday) "LOGGED TODAY" else "RECOVERING",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.TextSecondary
                )
            }
            Text(
                "~25 min · twice a week",
                style = MaterialTheme.typography.bodySmall,
                color = if (due) Palette.EmberText else Palette.TextSecondary
            )
        }

        if (due && nextWeights.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            nextWeights.take(3).forEach { next ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        next.exerciseName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(next.weightKg.kgDisplay(), style = NumeralSmall)
                    if (next.gainKg > 0.01) {
                        Text(
                            "+${next.gainKg.trimmed()}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Palette.Gain,
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Palette.GainSurface)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "You earned these last session. They're yours to claim.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.EmberText
            )
        } else if (!due) {
            Spacer(Modifier.height(8.dp))
            Text(
                training.nextSessionDate
                    ?.let { "Next session ${it.displayShort()} — ${training.restDaysBetween} rest days." }
                    ?: "No sessions logged yet. The first one sets your baseline.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextSecondary
            )
        }

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { onOpenPillar(Routes.SESSION) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (due) Palette.Ember else Palette.SurfaceRaised,
                contentColor = if (due) Palette.EmberInk else Palette.TextPrimary
            )
        ) {
            Text(
                if (due) "Start session" else "Train anyway",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (due) {
            // Shrinking the first step is the most reliable way past "not today" — almost
            // nobody stops after the warm-up.
            TextButton(
                onClick = { onOpenPillar(Routes.SESSION) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Not feeling it? Just do the warm-up — 3 min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.EmberText
                )
            }
        }
    }
}

/** Anticipation, not an afterthought: knowing it is coming is what makes today survivable. */
@Composable
private fun CheatDayStrip(motivation: MotivationState) {
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Palette.SurfaceRaised)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Palette.Nutrition)
        )
        Text(
            when (motivation.cheatDayIn) {
                0 -> "Cheat day is today. Damage control is in Nutrition — no guilt required."
                1 -> "Cheat day is ${motivation.cheatDayName} — tomorrow. Hold until then."
                else -> "Cheat day is ${motivation.cheatDayName} — ${motivation.cheatDayIn} days. Hold until then."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary
        )
    }
}

private fun LazyListScope.pillarRows(
    state: DashboardState,
    motivation: MotivationState?,
    onMarkDayClean: () -> Unit,
    onOpenPillar: (String) -> Unit
) {
    val settings = state.settings

    if (settings.isEnabled(Pillar.NUTRITION)) {
        item {
            val chain = motivation?.nutrition?.current ?: 0
            val clean = state.nutrition.rulesMet == 3
            ActionRow(
                colour = Palette.Nutrition,
                title = "Nutrition",
                detail = when {
                    state.nutrition.isCheatDay -> "Cheat day — damage control open"
                    clean -> "All three rules held today"
                    chain > 0 -> "$chain-day chain — one tap keeps it"
                    else -> "Log today to start a chain"
                },
                detailColour = if (!clean && chain > 0) Palette.Nutrition else Palette.TextSecondary,
                action = {
                    if (!clean && !state.nutrition.isCheatDay) {
                        // The compliant day costs one tap, right here.
                        OutlinedButton(
                            onClick = onMarkDayClean,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("All three held", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onOpenPillar(Routes.NUTRITION) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Open", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            )
        }
    }

    if (settings.isEnabled(Pillar.STRETCHES)) {
        item {
            ActionRow(
                colour = Palette.Stretches,
                title = "Desk reset",
                detail = if (state.stretches.deskResetCountToday > 0) {
                    "${state.stretches.deskResetCountToday} done today"
                } else {
                    "Nothing yet today — five minutes resets the hips"
                },
                action = {
                    OutlinedButton(
                        onClick = { onOpenPillar(Routes.deskReset(false)) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Start 5 min", style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        }
    }

    if (settings.isEnabled(Pillar.SLEEP)) {
        item {
            ActionRow(
                colour = Palette.Sleep,
                title = "Sleep",
                detail = if (state.sleep.logged) {
                    "Last night ${state.sleep.checksPassed} of 5 checks"
                } else {
                    "Night of ${state.sleep.nightDate.displayShort()} not logged"
                },
                action = {
                    OutlinedButton(
                        onClick = { onOpenPillar(Routes.SLEEP) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(if (state.sleep.logged) "Edit" else "Log", style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        }
    }

    if (settings.isEnabled(Pillar.CREATINE)) {
        item {
            val creatine = state.creatine
            ActionRow(
                colour = Palette.Creatine,
                title = "Creatine",
                detail = when {
                    creatine.cycleDay == null && creatine.cycleComplete -> "Cycle complete"
                    creatine.cycleDay == null -> "No cycle running"
                    creatine.morningTaken && creatine.eveningTaken ->
                        "Day ${creatine.cycleDay} of ${CreatineCycle.CYCLE_LENGTH_DAYS} — both taken"
                    else ->
                        "Day ${creatine.cycleDay} of ${CreatineCycle.CYCLE_LENGTH_DAYS} — " +
                            (if (creatine.morningTaken) "evening left" else "morning left")
                },
                action = {
                    OutlinedButton(
                        onClick = { onOpenPillar(Routes.CREATINE) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Open", style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        }
    }

    if (settings.isEnabled(Pillar.COLD)) {
        item {
            ActionRow(
                colour = Palette.Cold,
                title = "Cold exposure",
                detail = "${state.cold.countThisWeek} this week",
                action = {
                    OutlinedButton(
                        onClick = { onOpenPillar(Routes.COLD) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Log", style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        }
    }
}

private fun Double.trimmed(): String =
    if (this % 1.0 == 0.0) "${this.toInt()}" else "%.1f".format(this)
