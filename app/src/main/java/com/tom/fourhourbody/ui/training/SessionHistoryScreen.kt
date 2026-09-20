package com.tom.fourhourbody.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.ui.common.BackTopBar
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.util.displayShort

@Composable
fun SessionHistoryScreen(onBack: () -> Unit) {
    val container = rememberContainer()
    val viewModel: TrainingViewModel = viewModel(factory = TrainingViewModel.factory(container))
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar("Session history", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(session.date.displayShort(), style = MaterialTheme.typography.titleMedium)
                        Text(
                            when {
                                !session.completed -> "Abandoned"
                                session.stalled -> "Stalled"
                                else -> "Completed"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (session.notes != null) {
                            Text(
                                session.notes,
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
