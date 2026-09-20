package com.tom.fourhourbody.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.data.entity.DietMode
import com.tom.fourhourbody.data.entity.MealLogEntity
import com.tom.fourhourbody.data.entity.MealSlot
import com.tom.fourhourbody.data.reference.ReferenceDoc
import com.tom.fourhourbody.ui.common.CheckRow
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.SwitchRow
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.util.displayShort

@Composable
fun NutritionScreen(onOpenReference: (ReferenceDoc) -> Unit) {
    val container = rememberContainer()
    val viewModel: NutritionViewModel = viewModel(factory = NutritionViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Nutrition", style = MaterialTheme.typography.headlineMedium)
                Text(
                    state.date.displayShort(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SectionCard(title = "Mode") {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.mode == DietMode.SLOW_CARB,
                            onClick = { viewModel.setMode(DietMode.SLOW_CARB) },
                            label = { Text("Slow-Carb") }
                        )
                        FilterChip(
                            selected = state.mode == DietMode.HYBRID,
                            onClick = { viewModel.setMode(DietMode.HYBRID) },
                            label = { Text("Hybrid") }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.isTrainingDay) {
                            "Training day — rice is available in Hybrid mode."
                        } else {
                            "Rest day — no rice in either mode."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SectionCard(title = "Cheat day") {
                SwitchRow(
                    label = "Today is the cheat day",
                    checked = state.day?.isCheatDay == true,
                    onCheckedChange = viewModel::setCheatDay,
                    supporting = "Swaps the rule checklist for damage control. Nothing already " +
                        "logged for today is lost either way."
                )
            }
        }

        if (state.day?.isCheatDay == true) {
            item {
                SectionCard(
                    title = "Damage control",
                    subtitle = "Optional taps. Two of five is better than none — this is not a " +
                        "requirement and not a guilt trip."
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
        } else {
            item {
                SectionCard(
                    title = "Rules",
                    subtitle = if (state.mode == DietMode.HYBRID) {
                        "Hybrid: rice on training days, everything else unchanged."
                    } else {
                        "Slow-Carb, strict."
                    }
                ) {
                    val day = state.day
                    Column {
                        CheckRow(
                            label = "No white starchy carbs",
                            checked = day?.avoidedWhiteCarbs == true,
                            onCheckedChange = { value ->
                                viewModel.setRule { it.copy(avoidedWhiteCarbs = value) }
                            },
                            supporting = if (state.riceAvailable) {
                                "Rice with the training meal is allowed today."
                            } else {
                                null
                            }
                        )
                        CheckRow(
                            label = "No liquid calories",
                            checked = day?.noLiquidCalories == true,
                            onCheckedChange = { value ->
                                viewModel.setRule { it.copy(noLiquidCalories = value) }
                            }
                        )
                        CheckRow(
                            label = "No fruit",
                            checked = day?.noFruit == true,
                            onCheckedChange = { value -> viewModel.setRule { it.copy(noFruit = value) } },
                            supporting = "Tomato and avocado excepted."
                        )
                    }
                }
            }
        }

        item { Text("Meals", style = MaterialTheme.typography.titleMedium) }

        MealSlot.entries.forEach { slot ->
            item(key = "meal-$slot") {
                MealCard(
                    slot = slot,
                    meal = state.meals.firstOrNull { it.mealSlot == slot },
                    riceAvailable = state.riceAvailable,
                    onChange = { transform -> viewModel.updateMeal(slot, transform) }
                )
            }
        }

        item { Text("Reference", style = MaterialTheme.typography.titleMedium) }

        // The sleep reference belongs to the Sleep pillar, not to this screen.
        ReferenceDoc.entries.filterNot { it == ReferenceDoc.SLEEP }.forEach { doc ->
            item(key = "ref-${doc.name}") {
                SectionCard(title = doc.title, onClick = { onOpenReference(doc) })
            }
        }
    }
}

@Composable
private fun MealCard(
    slot: MealSlot,
    meal: MealLogEntity?,
    riceAvailable: Boolean,
    onChange: ((MealLogEntity) -> MealLogEntity) -> Unit
) {
    SectionCard(title = slot.name.lowercase().replaceFirstChar { it.uppercase() }) {
        Column {
            TagField("Protein", meal?.proteinTag) { value ->
                onChange { it.copy(proteinTag = value) }
            }
            Spacer(Modifier.height(8.dp))
            TagField("Legume", meal?.legumeTag) { value ->
                onChange { it.copy(legumeTag = value) }
            }
            Spacer(Modifier.height(8.dp))
            TagField("Vegetable", meal?.vegTag) { value ->
                onChange { it.copy(vegTag = value) }
            }
            Spacer(Modifier.height(8.dp))
            if (riceAvailable) {
                CheckRow(
                    label = "Rice included",
                    checked = meal?.riceIncluded == true,
                    onCheckedChange = { value -> onChange { it.copy(riceIncluded = value) } }
                )
            }
            CheckRow(
                label = "Soup",
                checked = meal?.soupUsed == true,
                onCheckedChange = { value -> onChange { it.copy(soupUsed = value) } }
            )
        }
    }
}

@Composable
private fun TagField(label: String, value: String?, onCommit: (String) -> Unit) {
    // Local state so typing never fights the database round-trip.
    var text by remember(label, value == null) { mutableStateOf(value.orEmpty()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onCommit(it)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
