package com.tom.fourhourbody.ui.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.domain.deck.CardTier
import com.tom.fourhourbody.domain.deck.DeckCard
import com.tom.fourhourbody.domain.progress.Attribute
import com.tom.fourhourbody.domain.progress.Milestone
import com.tom.fourhourbody.domain.progress.Milestones
import com.tom.fourhourbody.domain.progress.Stats
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.ui.theme.Palette
import com.tom.fourhourbody.ui.theme.PanelShape
import com.tom.fourhourbody.ui.theme.RunicLabel
import com.tom.fourhourbody.ui.theme.RunicNumeral
import com.tom.fourhourbody.ui.theme.RunicTag
import com.tom.fourhourbody.ui.theme.RunicValue
import com.tom.fourhourbody.ui.theme.RunicTitle
import com.tom.fourhourbody.util.kgDisplay

/**
 * The character sheet.
 *
 * An action RPG's character panel is a readout of what the player did: attributes derived from
 * history, gear describing the loadout, a quest log of what is nearly done. This app already
 * held all three and was showing them as a tracker, so what changed here is the language and
 * the frame, not the arithmetic. Every number on this screen was already in the database.
 *
 * The ornament stops at this screen. Today stays bare, because one is inspected and the other
 * is acted on.
 */
@Composable
fun DeckScreen() {
    val container = rememberContainer()
    val viewModel: DeckViewModel = viewModel(factory = DeckViewModel.factory(container))
    val deck by viewModel.deck.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val attributes by viewModel.attributes.collectAsStateWithLifecycle()

    var inspecting by remember { mutableStateOf<DeckCard?>(null) }

    val current = deck

    Box(Modifier.fillMaxSize().background(Palette.Ground2)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { LevelPlate(stats) }

            if (attributes.isNotEmpty()) {
                item {
                    Panel("ATTRIBUTES") {
                        attributes.forEach { AttributeRow(it) }
                    }
                }
            }

            if (current != null) {
                item {
                    Panel("LOADOUT") {
                        current.cards.forEach { card ->
                            ItemRow(card) { inspecting = card }
                        }
                    }
                }
            }

            val quests = Milestones.questLog(stats)
            if (quests.isNotEmpty()) {
                item {
                    Panel("QUEST LOG") {
                        quests.forEach { QuestRow(it, stats) }
                    }
                }
            }
        }
    }

    inspecting?.let { card ->
        ItemTooltip(card) { inspecting = null }
    }
}

// ---------------------------------------------------------------------------------------
// Frames
// ---------------------------------------------------------------------------------------

@Composable
private fun Panel(
    header: String,
    lit: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(if (lit) Palette.PanelLit else Palette.PanelDark)
            .border(1.dp, if (lit) Palette.BrassDim else Palette.RuleDark, PanelShape)
            .padding(14.dp)
    ) {
        Text(header, style = RunicLabel, color = Palette.BrassDim)
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.RuleDark))
        Spacer(Modifier.height(9.dp))
        content()
    }
}

@Composable
private fun LevelPlate(stats: Stats) {
    val level = Milestones.level(stats)
    val next = Milestones.nextUp(stats)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Palette.PanelLit)
            .border(1.dp, Palette.BrassDim, PanelShape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("LEVEL", style = RunicLabel, color = Palette.BrassDim)
                Text("$level", style = RunicNumeral, color = Palette.Brass)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("BIG FIVE", style = RunicTitle, color = Palette.Parchment)
                Spacer(Modifier.height(3.dp))
                Text(
                    "$level of ${Milestones.ALL.size} marks earned",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.ParchmentFaint
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            next?.let { "Next mark: ${it.title.lowercase()}" }
                ?: "Every mark earned. Nothing left to ask of you.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.ParchmentDim
        )
    }
}

// ---------------------------------------------------------------------------------------
// Attributes — each carries the count it came from, so it can always be checked.
// ---------------------------------------------------------------------------------------

@Composable
private fun AttributeRow(attribute: Attribute) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            attribute.name.uppercase(),
            style = RunicLabel,
            color = Palette.ParchmentDim,
            modifier = Modifier.width(92.dp)
        )
        Text(
            "${attribute.value}",
            style = RunicValue,
            color = Palette.Parchment,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            attribute.source,
            style = MaterialTheme.typography.bodySmall,
            color = Palette.ParchmentFaint,
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------------------------------------------------------------------------------------
// Items — exercises and protocols, with the tier as rarity.
// ---------------------------------------------------------------------------------------

@Composable
private fun ItemRow(card: DeckCard, onClick: () -> Unit) {
    val rarity = if (card.inDeck) Palette.ofTier(card.tier) else Palette.RarityUnplayed

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Gem(filled = card.inDeck && card.tier != CardTier.UNPLAYED, colour = rarity)
        Spacer(Modifier.width(10.dp))
        Text(
            card.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (card.inDeck) Palette.Parchment else Palette.ParchmentFaint,
            modifier = Modifier.weight(1f)
        )
        card.gainKg?.takeIf { it > 0.01 }?.let { gain ->
            Text(
                "+${gain.kgDisplay()}",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.ParchmentFaint
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            if (card.inDeck) card.tier.label.uppercase() else "BENCHED",
            style = RunicTag,
            color = rarity
        )
    }
}

/** A diamond rather than a dot — the socket an ARPG puts a gem in. */
@Composable
private fun Gem(filled: Boolean, colour: Color) {
    Box(
        Modifier
            .size(9.dp)
            .rotate(45f)
            .then(
                if (filled) {
                    Modifier.background(colour)
                } else {
                    Modifier.border(1.dp, colour)
                }
            )
    )
}

/**
 * The tooltip every action RPG player reads without thinking about it. The affixes are the
 * exercise's own history — nothing here is generated.
 */
@Composable
private fun ItemTooltip(card: DeckCard, onDismiss: () -> Unit) {
    val rarity = Palette.ofTier(card.tier)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Palette.Ground2)
                .border(1.dp, rarity)
                .padding(16.dp)
        ) {
            Text(
                card.name.uppercase(),
                style = RunicTitle,
                color = rarity,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Strength slot",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.ParchmentFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.RuleDark))
            Spacer(Modifier.height(12.dp))

            Text(card.detail, style = MaterialTheme.typography.bodyMedium, color = Palette.Parchment)
            card.gainKg?.takeIf { it > 0.01 }?.let { gain ->
                Spacer(Modifier.height(5.dp))
                Text(
                    "+${gain.kgDisplay()} since first logged",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.RarityOpening
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                "${card.tier.label} · ${card.plays} ${if (card.plays == 1) "play" else "plays"}",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.RarityOpening
            )

            Spacer(Modifier.height(14.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close", color = Palette.BrassDim)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------
// Quests and combos
// ---------------------------------------------------------------------------------------

@Composable
private fun QuestRow(quest: Milestone, stats: Stats) {
    val current = stats.current(quest.track)

    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                quest.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Parchment,
                modifier = Modifier.weight(1f)
            )
            Text(
                "$current / ${quest.target}",
                style = RunicLabel,
                color = Palette.Brass
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Palette.Ground2)
        ) {
            val fraction = quest.fraction(stats)
            if (fraction > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(3.dp)
                        .background(Palette.Brass)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            quest.detail,
            style = MaterialTheme.typography.bodySmall,
            color = Palette.ParchmentFaint
        )
    }
}
