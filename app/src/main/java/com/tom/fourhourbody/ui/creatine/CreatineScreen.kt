package com.tom.fourhourbody.ui.creatine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.domain.creatine.CreatineCycle
import com.tom.fourhourbody.ui.common.CheckRow
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.util.displayShort
import java.time.LocalDate

/** A log, not an advisor: two taps a day and a visible cycle counter. */
@Composable
fun CreatineScreen() {
    val container = rememberContainer()
    val viewModel: CreatineViewModel = viewModel(factory = CreatineViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Creatine", style = MaterialTheme.typography.headlineMedium)
                Text(
                    when {
                        state.cycle.day != null ->
                            "Day ${state.cycle.day} of ${CreatineCycle.CYCLE_LENGTH_DAYS}"
                        state.cycle.complete -> "Cycle complete"
                        else -> "No cycle running"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.cycle.day != null) {
            item {
                SectionCard(
                    title = "Today",
                    subtitle = "${CreatineCycle.DOSE_GRAMS} g on waking, " +
                        "${CreatineCycle.DOSE_GRAMS} g before bed."
                ) {
                    Column {
                        CheckRow(
                            label = "Morning",
                            checked = state.log?.morningTaken == true,
                            onCheckedChange = viewModel::setMorning
                        )
                        CheckRow(
                            label = "Evening",
                            checked = state.log?.eveningTaken == true,
                            onCheckedChange = viewModel::setEvening
                        )
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Cycle",
                subtitle = state.cycle.startDate
                    ?.let { "Started ${it.displayShort()}, ends ${CreatineCycle.endDate(it).displayShort()}." }
                    ?: "Start a 28-day cycle whenever you like."
            ) {
                Button(
                    onClick = { viewModel.startNewCycle(LocalDate.now()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.cycle.started) "Start a new cycle today" else "Start cycle today")
                }
            }
        }
    }
}
