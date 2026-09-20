package com.tom.fourhourbody.ui.stretches

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.data.entity.StretchMode
import com.tom.fourhourbody.ui.common.BackTopBar
import com.tom.fourhourbody.ui.common.NumberField
import com.tom.fourhourbody.ui.common.rememberContainer

/**
 * Editing a hold time or a rep target here changes every entry point that uses the stretch —
 * inline from a session and standalone alike — with no rebuild.
 */
@Composable
fun StretchConfigScreen(onBack: () -> Unit) {
    val container = rememberContainer()
    val viewModel: StretchConfigViewModel =
        viewModel(factory = StretchConfigViewModel.factory(container))
    val configs by viewModel.configs.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar("Stretch settings", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(configs, key = { it.id }) { config ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(config.stretchName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    config.routine.label +
                                        if (config.isWeeklyOnly) " · weekly set" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = config.isActive,
                                onCheckedChange = { viewModel.update(config.copy(isActive = it)) }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (config.mode) {
                                StretchMode.HOLD -> NumberField(
                                    label = "Hold (sec)",
                                    value = config.defaultHoldSec?.toString().orEmpty(),
                                    onValueChange = { raw ->
                                        viewModel.update(
                                            config.copy(defaultHoldSec = raw.toIntOrNull())
                                        )
                                    },
                                    modifier = Modifier.width(140.dp)
                                )

                                StretchMode.REPS -> {
                                    NumberField(
                                        label = "Reps",
                                        value = config.defaultReps?.toString().orEmpty(),
                                        onValueChange = { raw ->
                                            viewModel.update(config.copy(defaultReps = raw.toIntOrNull()))
                                        },
                                        modifier = Modifier.width(110.dp)
                                    )
                                    NumberField(
                                        label = "Sets",
                                        value = config.defaultSets?.toString().orEmpty(),
                                        onValueChange = { raw ->
                                            viewModel.update(config.copy(defaultSets = raw.toIntOrNull()))
                                        },
                                        modifier = Modifier.width(110.dp)
                                    )
                                }
                            }
                        }

                        if (config.notes != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                config.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
