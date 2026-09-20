package com.tom.fourhourbody.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tom.fourhourbody.data.reference.ReferenceDoc
import com.tom.fourhourbody.ui.common.BackTopBar
import com.tom.fourhourbody.ui.common.rememberContainer
import kotlinx.coroutines.launch

/**
 * Reference content is a bundled markdown asset with an editable override, so rules or the
 * meal plan can be changed on the device without a rebuild. Rendering is deliberately plain
 * text — a markdown engine would be a dependency for very little gain here.
 */
@Composable
fun ReferenceScreen(doc: ReferenceDoc, onBack: () -> Unit) {
    val container = rememberContainer()
    val scope = rememberCoroutineScope()

    var content by remember(doc) { mutableStateOf<String?>(null) }
    var edited by remember(doc) { mutableStateOf(false) }
    var editing by remember(doc) { mutableStateOf(false) }
    var draft by remember(doc) { mutableStateOf("") }

    LaunchedEffect(doc) {
        content = container.referenceRepository.read(doc)
        edited = container.referenceRepository.isEdited(doc)
    }

    Scaffold(
        topBar = {
            BackTopBar(doc.title, onBack) {
                if (!editing) {
                    TextButton(
                        onClick = {
                            draft = content.orEmpty()
                            editing = true
                        }
                    ) { Text("Edit") }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (editing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Markdown") }
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                container.referenceRepository.write(doc, draft)
                                content = draft
                                edited = true
                                editing = false
                            }
                        }
                    ) { Text("Save") }
                    OutlinedButton(onClick = { editing = false }) { Text("Cancel") }
                }
            } else {
                Text(content ?: "Loading…", style = MaterialTheme.typography.bodyMedium)
                if (edited) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Edited on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                container.referenceRepository.revert(doc)
                                content = container.referenceRepository.read(doc)
                                edited = false
                            }
                        }
                    ) { Text("Revert to bundled text") }
                }
            }
        }
    }
}
