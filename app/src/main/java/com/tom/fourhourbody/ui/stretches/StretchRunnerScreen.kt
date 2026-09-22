package com.tom.fourhourbody.ui.stretches

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.data.entity.StretchRoutine
import com.tom.fourhourbody.ui.common.rememberContainer

/**
 * Standalone entry point for a routine — rest-day mobility or a desk reset. Same engine the
 * training session calls inline; the only difference is that these logs carry no session id.
 */
@Composable
fun StretchRunnerScreen(
    routine: StretchRoutine,
    weekly: Boolean,
    onExit: () -> Unit
) {
    val container = rememberContainer()
    val viewModel: StretchRunnerViewModel = viewModel(
        key = "stretch-$routine-$weekly",
        factory = StretchRunnerViewModel.factory(container, routine, weekly)
    )
    val steps by viewModel.steps.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (saved) onExit()
    }

    val currentSteps = steps
    if (currentSteps == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading…") }
        return
    }

    KeepScreenOn()

    // Inside a session the runner already sits in a scrolling column; standalone it did not,
    // so anything taller than the screen — the how-to panel, once opened — was unreachable.
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StretchRunner(
            steps = currentSteps,
            onFinished = { completions -> viewModel.save(completions) },
            onExit = onExit
        )
    }
}
