package com.tom.fourhourbody.ui.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.data.entity.DietMode
import com.tom.fourhourbody.data.reference.ReferenceDoc
import com.tom.fourhourbody.ui.common.CheckRow
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.SwitchRow
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.ui.theme.NumeralSmall
import com.tom.fourhourbody.ui.theme.Palette
import com.tom.fourhourbody.util.displayShort

/**
 * Rule compliance, not calories. The book's own position is that Slow-Carb works without
 * counting anything, and a dedicated tracker handles the macro side better than this app
 * ever would — so what is logged here is the question that tracker cannot answer: did the
 * four rules hold today.
 */
@Composable
fun NutritionScreen(onOpenReference: (ReferenceDoc) -> Unit) {
    val container = rememberContainer()
    val viewModel: NutritionViewModel = viewModel(factory = NutritionViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    val cheatDay = state.day?.isCheatDay == true

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Nutrition", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${state.date.displayShort()} · ${
                        if (state.mode == DietMode.HYBRID) "Hybrid" else "Slow-Carb, strict"
                    }",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextSecondary
                )
            }
        }

        if (!cheatDay) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (state.allRulesHeld) Palette.Surface else Palette.EmberSurface)
                        .then(
                            if (state.allRulesHeld) {
                                Modifier
                            } else {
                                Modifier.border(1.dp, Palette.EmberLine, RoundedCornerShape(16.dp))
                            }
                        )
                        .padding(18.dp)
                ) {
                    Text(
                        if (state.allRulesHeld) "TODAY IS LOGGED" else "THE WHOLE DAY, ONE TAP",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.allRulesHeld) Palette.TextSecondary else Palette.EmberText
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.allRulesHeld) {
                            "All three rules held."
                        } else {
                            "No white carbs, nothing drunk with calories, no fruit."
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = viewModel::markDayClean,
                        enabled = !state.allRulesHeld,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Palette.Nutrition,
                            contentColor = Palette.EmberInk,
                            disabledContainerColor = Palette.SurfaceRaised,
                            disabledContentColor = Palette.TextSecondary
                        )
                    ) {
                        Text(
                            if (state.allRulesHeld) "Logged — nothing more to do" else "All three held",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                SectionCard(
                    title = "Or mark what slipped",
                    subtitle = "Only the exception costs more than a tap."
                ) {
                    Column {
                        CheckRow(
                            label = "No white starchy carbs",
                            checked = state.day?.avoidedWhiteCarbs == true,
                            onCheckedChange = { value ->
                                viewModel.setRule { it.copy(avoidedWhiteCarbs = value) }
                            },
                            supporting = if (state.mode == DietMode.HYBRID && state.isTrainingDay) {
                                "Training day — rice around the session is allowed in Hybrid."
                            } else {
                                null
                            }
                        )
                        CheckRow(
                            label = "No liquid calories",
                            checked = state.day?.noLiquidCalories == true,
                            onCheckedChange = { value ->
                                viewModel.setRule { it.copy(noLiquidCalories = value) }
                            }
                        )
                        CheckRow(
                            label = "No fruit",
                            checked = state.day?.noFruit == true,
                            onCheckedChange = { value -> viewModel.setRule { it.copy(noFruit = value) } },
                            supporting = "Tomato and avocado excepted."
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "Mode") {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.mode == DietMode.SLOW_CARB,
                            onClick = { viewModel.setMode(DietMode.SLOW_CARB) },
                            label = { Text("Slow-Carb") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Palette.Nutrition,
                                selectedLabelColor = Palette.EmberInk
                            )
                        )
                        FilterChip(
                            selected = state.mode == DietMode.HYBRID,
                            onClick = { viewModel.setMode(DietMode.HYBRID) },
                            label = { Text("Hybrid") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Palette.Nutrition,
                                selectedLabelColor = Palette.EmberInk
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.isTrainingDay) {
                            "Training day — Hybrid allows rice around the session."
                        } else {
                            "Rest day — no rice in either mode."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextSecondary
                    )
                }
            }
        }

        item {
            SectionCard(title = "Cheat day") {
                SwitchRow(
                    label = "Today is the cheat day",
                    checked = cheatDay,
                    onCheckedChange = viewModel::setCheatDay,
                    supporting = "Swaps the rules for damage control. It is part of the plan, " +
                        "and a logged cheat day still counts towards your chain."
                )
            }
        }

        if (cheatDay) {
            item {
                SectionCard(
                    title = "Damage control",
                    subtitle = "Optional taps. Two of five beats none — this is not a scorecard."
                ) {
                    val damage = state.damageControl
                    Column {
                        CheckRow(
                            label = "High protein/fibre first meal",
                            checked = damage?.proteinFiberFirstMeal == true,
                            onCheckedChange = { value ->
                                viewModel.updateDamageControl { it.copy(proteinFiberFirstMeal = value) }
                            }
                        )
                        CheckRow(
                            label = "Citrus before the big meal",
                            checked = damage?.citrusBeforeBigMeal == true,
                            onCheckedChange = { value ->
                                viewModel.updateDamageControl { it.copy(citrusBeforeBigMeal = value) }
                            },
                            supporting = "Grapefruit, lemon or lime."
                        )
                        CheckRow(
                            label = "60–90s movement before the meal",
                            checked = damage?.movementBeforeMeal == true,
                            onCheckedChange = { value ->
                                viewModel.updateDamageControl { it.copy(movementBeforeMeal = value) }
                            },
                            supporting = "Air squats or wall presses."
                        )
                        CheckRow(
                            label = "60–90s movement ~90 min after",
                            checked = damage?.movementAfterMeal == true,
                            onCheckedChange = { value ->
                                viewModel.updateDamageControl { it.copy(movementAfterMeal = value) }
                            }
                        )
                        CheckRow(
                            label = "Walk afterwards",
                            checked = damage?.walkedAfterMeal == true,
                            onCheckedChange = { value ->
                                viewModel.updateDamageControl { it.copy(walkedAfterMeal = value) }
                            }
                        )
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Reference", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${ReferenceDoc.entries.size - 1}",
                    style = NumeralSmall,
                    color = Palette.TextTertiary
                )
            }
        }

        // The sleep reference belongs to the Sleep pillar, not to this screen.
        ReferenceDoc.entries.filterNot { it == ReferenceDoc.SLEEP }.forEach { doc ->
            item(key = "ref-${doc.name}") {
                SectionCard(title = doc.title, onClick = { onOpenReference(doc) })
            }
        }
    }
}
