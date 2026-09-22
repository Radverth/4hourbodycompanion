package com.tom.fourhourbody.ui.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.ui.common.CheckRow
import com.tom.fourhourbody.ui.common.ChipRow
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.util.displayShort

@Composable
fun SleepScreen() {
    val container = rememberContainer()
    val viewModel: SleepViewModel = viewModel(factory = SleepViewModel.factory(container))
    val log by viewModel.log.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Sleep", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Night of ${viewModel.nightDate.displayShort()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SectionCard(title = "Checklist") {
                Column {
                    CheckRow(
                        label = "Room at 67–70°F",
                        checked = log?.roomTempOk == true,
                        onCheckedChange = { value -> viewModel.update { it.copy(roomTempOk = value) } },
                        supporting = "19–21°C."
                    )
                    CheckRow(
                        label = "Socks (65°F alternative)",
                        checked = log?.socksUsed == true,
                        onCheckedChange = { value -> viewModel.update { it.copy(socksUsed = value) } },
                        supporting = "Counts in place of the temperature check — warm feet let " +
                            "the body shed core heat."
                    )
                    CheckRow(
                        label = "Dark",
                        checked = log?.darkness == true,
                        onCheckedChange = { value -> viewModel.update { it.copy(darkness = value) } }
                    )
                    CheckRow(
                        label = "No screens before bed",
                        checked = log?.noScreensBeforeBed == true,
                        onCheckedChange = { value ->
                            viewModel.update { it.copy(noScreensBeforeBed = value) }
                        }
                    )
                    CheckRow(
                        label = "Wine within limit",
                        checked = log?.wineWithinLimit == true,
                        onCheckedChange = { value ->
                            viewModel.update { it.copy(wineWithinLimit = value) }
                        },
                        supporting = "Two glasses or fewer AND finished 4+ hours before bed — " +
                            "one check, because the timing is what the book found actually matters."
                    )
                    CheckRow(
                        label = "Cold exposure before bed",
                        checked = log?.coldExposureBeforeBed == true,
                        onCheckedChange = { value ->
                            viewModel.update { it.copy(coldExposureBeforeBed = value) }
                        },
                        supporting = "Optional. Log the bath itself in Cold exposure."
                    )
                    CheckRow(
                        label = "Consistent wake time",
                        checked = log?.consistentWakeTime == true,
                        onCheckedChange = { value ->
                            viewModel.update { it.copy(consistentWakeTime = value) }
                        }
                    )
                }
            }
        }

        item {
            SectionCard(title = "Quality", subtitle = "Optional, 1–5.") {
                ChipRow {
                    (1..5).forEach { rating ->
                        FilterChip(
                            selected = log?.qualityRating == rating,
                            onClick = {
                                viewModel.update {
                                    it.copy(
                                        qualityRating = if (it.qualityRating == rating) null else rating
                                    )
                                }
                            },
                            label = { Text("$rating") }
                        )
                    }
                }
            }
        }
    }
}
