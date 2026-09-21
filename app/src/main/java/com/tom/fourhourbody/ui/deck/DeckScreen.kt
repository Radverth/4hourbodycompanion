package com.tom.fourhourbody.ui.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.domain.deck.CardKind
import com.tom.fourhourbody.domain.deck.CardTier
import com.tom.fourhourbody.domain.deck.Deck
import com.tom.fourhourbody.domain.deck.DeckCard
import com.tom.fourhourbody.domain.synergy.SynergyState
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.ui.theme.NumeralLarge
import com.tom.fourhourbody.ui.theme.Palette
import com.tom.fourhourbody.util.kgDisplay

/**
 * The deck: every protocol and every exercise you own, as cards, in one place.
 *
 * The point of collecting them here is that the app's commitments are otherwise scattered
 * across six screens, so it is impossible to see what you are actually running. A tier is
 * only ever a reading of the log — the number that produced it sits on the card, and a card
 * is benched rather than deleted, so nothing you built is ever thrown away.
 */
@Composable
fun DeckScreen() {
    val container = rememberContainer()
    val viewModel: DeckViewModel = viewModel(factory = DeckViewModel.factory(container))
    val deck by viewModel.deck.collectAsStateWithLifecycle()
    val synergies by viewModel.synergies.collectAsStateWithLifecycle()

    val current = deck

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Your deck", style = MaterialTheme.typography.headlineMedium) }

        if (current == null) {
            item { Text("Loading…", color = Palette.TextSecondary) }
            return@LazyColumn
        }

        item { DeckHeadline(current) }

        if (synergies.isNotEmpty()) {
            item {
                SectionLabel("COMBOS")
                Text(
                    "Pairings the book itself makes, where doing both does more than doing " +
                        "either. Nothing is scored — these only report what landed together.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary
                )
            }
            items(synergies, key = { it.synergy.id }) { state -> SynergyRow(state) }
            item { Spacer(Modifier.height(6.dp)) }
        }

        item { SectionLabel("IN PLAY") }
        cardRows(current.inDeck, viewModel::toggle)

        if (current.bench.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                SectionLabel("BENCHED")
                Text(
                    "Still yours. Tap one to put it back — it returns at the tier it earned.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary
                )
            }
            cardRows(current.bench, viewModel::toggle)
        }
    }
}

/** Two cards to a row, built from plain Rows so the layout uses nothing exotic. */
private fun LazyListScope.cardRows(
    cards: List<DeckCard>,
    onToggle: (DeckCard) -> Unit
) {
    val rows = cards.chunked(2)
    rows.forEachIndexed { index, row ->
        item(key = "row-${row.first().id}-$index") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { card ->
                    CardTile(card, Modifier.weight(1f)) { onToggle(card) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DeckHeadline(deck: Deck) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${deck.inDeck.size}", style = NumeralLarge)
            Text(
                " cards in play",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Text(
            "${deck.played} of them have actually been run. A card you have never played " +
                "counts for nothing until you do.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Palette.TextTertiary)
}

@Composable
private fun CardTile(card: DeckCard, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colour = Palette.of(card.pillar)
    val faded = !card.inDeck || card.tier == CardTier.UNPLAYED

    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (card.inDeck) Palette.Surface else Palette.Ground)
            .border(
                width = 1.dp,
                color = if (card.inDeck) Palette.LineStrong else Palette.Line,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TierPips(card.tier, if (faded) Palette.DotEmpty else colour)
            Spacer(Modifier.weight(1f))
            if (card.kind == CardKind.PILLAR) {
                Text(
                    "PROTOCOL",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.TextTertiary
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            card.tier.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (faded) Palette.TextTertiary else colour
        )
        Text(
            card.name,
            style = MaterialTheme.typography.titleMedium,
            color = if (card.inDeck) Palette.TextPrimary else Palette.TextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(card.detail, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary)

        card.gainKg?.takeIf { it > 0.01 }?.let { gain ->
            Spacer(Modifier.height(6.dp))
            Text(
                "+${gain.kgDisplay()} since the first time",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Gain
            )
        }
    }
}

/** Three pips, one per tier above unplayed. Readable without reading the label. */
@Composable
private fun TierPips(tier: CardTier, colour: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(CardTier.entries.size - 1) { index ->
            Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (index < tier.ordinal) colour else Palette.DotEmpty)
            )
        }
    }
}

/**
 * One combo. A synergy that has fired today says so; one with a half still open says which
 * half — that is the only part of this worth acting on, so it gets the accent.
 */
@Composable
private fun SynergyRow(state: SynergyState) {
    val synergy = state.synergy
    val live = state.halfOpen
    val accent = if (live) Palette.Ember else Palette.of(synergy.pillars.first())

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (live) Palette.EmberSurface else Palette.Surface)
            .border(
                width = 1.dp,
                color = if (live) Palette.EmberLine else Palette.Line,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            synergy.pillars.forEach { pillar ->
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Palette.of(pillar))
                )
                Spacer(Modifier.size(5.dp))
            }
            Text(
                synergy.name,
                style = MaterialTheme.typography.titleMedium,
                color = Palette.TextPrimary
            )
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    state.firedToday -> "TODAY"
                    state.everFired -> "${state.timesInWindow}× in ${state.windowDays}d"
                    else -> "NOT YET"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (state.firedToday) accent else Palette.TextTertiary
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(synergy.what, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        Spacer(Modifier.height(6.dp))
        Text(synergy.why, style = MaterialTheme.typography.bodySmall, color = Palette.TextTertiary)

        if (live) {
            Spacer(Modifier.height(10.dp))
            Text(
                synergy.prompt,
                style = MaterialTheme.typography.labelLarge,
                color = Palette.Ember
            )
        }
    }
}
