package com.tom.fourhourbody.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.domain.training.TrainingConstants
import com.tom.fourhourbody.ui.common.BackTopBar
import com.tom.fourhourbody.ui.common.NumberField
import com.tom.fourhourbody.ui.common.rememberContainer

@Composable
fun ExerciseConfigScreen(onBack: () -> Unit) {
    val container = rememberContainer()
    val viewModel: TrainingViewModel = viewModel(factory = TrainingViewModel.factory(container))
    val configs by viewModel.configs.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar("Exercises", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Leg press carries the book's ${TrainingConstants.LEG_PRESS_TARGET_REPS}+ rep " +
                        "target; everything else is ${TrainingConstants.DEFAULT_TARGET_REPS}+. " +
                        "Kettlebell swings run as the Tabata block, not as a tempo set.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(configs, key = { it.id }) { config ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(config.slotName, style = MaterialTheme.typography.labelLarge)
                            Switch(
                                checked = config.isActive,
                                onCheckedChange = { viewModel.updateConfig(config.copy(isActive = it)) }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = config.exerciseName,
                            onValueChange = { viewModel.updateConfig(config.copy(exerciseName = it)) },
                            label = { Text("Exercise") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = config.equipment,
                                onValueChange = {
                                    viewModel.updateConfig(config.copy(equipment = it))
                                },
                                label = { Text("Equipment") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            NumberField(
                                label = "Target reps",
                                value = config.targetReps.toString(),
                                onValueChange = { raw ->
                                    raw.toIntOrNull()?.let {
                                        viewModel.updateConfig(config.copy(targetReps = it))
                                    }
                                },
                                modifier = Modifier.width(130.dp)
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(onClick = viewModel::addConfig, modifier = Modifier.fillMaxWidth()) {
                    Text("Add exercise")
                }
            }
        }
    }
}
