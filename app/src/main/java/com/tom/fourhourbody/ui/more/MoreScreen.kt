package com.tom.fourhourbody.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.nav.Routes

@Composable
fun MoreScreen(onOpen: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("More", style = MaterialTheme.typography.headlineMedium) }
        item {
            SectionCard(
                title = "Progress",
                subtitle = "Weight, waist, hip, photos and trends.",
                onClick = { onOpen(Routes.PROGRESS) }
            )
        }
        item {
            SectionCard(
                title = "Settings",
                subtitle = "Reminders and defaults.",
                onClick = { onOpen(Routes.SETTINGS) }
            )
        }
    }
}
