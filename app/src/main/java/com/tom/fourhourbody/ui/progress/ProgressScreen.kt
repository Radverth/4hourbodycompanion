package com.tom.fourhourbody.ui.progress

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom.fourhourbody.ui.common.NumberField
import com.tom.fourhourbody.ui.common.SectionCard
import com.tom.fourhourbody.ui.common.rememberContainer
import com.tom.fourhourbody.util.PhotoStore
import com.tom.fourhourbody.util.displayShort
import java.io.File

@Composable
fun ProgressScreen() {
    val container = rememberContainer()
    val viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.factory(container))
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val latest by viewModel.latest.collectAsStateWithLifecycle()

    var weight by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var hip by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Progress", style = MaterialTheme.typography.headlineMedium) }

        item {
            SectionCard(
                title = "Latest",
                subtitle = latest?.let { entry ->
                    buildString {
                        append(entry.date.displayShort())
                        entry.weightKg?.let { append(" · %.1f kg".format(it)) }
                        entry.waistCm?.let { append(" · waist %.1f cm".format(it)) }
                        entry.hipCm?.let { append(" · hip %.1f cm".format(it)) }
                    }
                } ?: "Nothing logged yet."
            )
        }

        item {
            SectionCard(title = "Today's measurements") {
                Column {
                    NumberField(
                        label = "Weight (kg)",
                        value = weight,
                        onValueChange = { weight = it },
                        decimal = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    NumberField(
                        label = "Waist (cm)",
                        value = waist,
                        onValueChange = { waist = it },
                        decimal = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    NumberField(
                        label = "Hip (cm)",
                        value = hip,
                        onValueChange = { hip = it },
                        decimal = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.saveMeasurement(
                                weight.toDoubleOrNull(),
                                waist.toDoubleOrNull(),
                                hip.toDoubleOrNull()
                            )
                            weight = ""
                            waist = ""
                            hip = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save") }
                }
            }
        }

        item {
            SectionCard(title = "Photos", subtitle = "Stored on the device, never uploaded.") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PhotoSlotControl(
                        slot = PhotoSlot.FRONT,
                        path = latest?.photoUriFront,
                        onPicked = { viewModel.setPhoto(PhotoSlot.FRONT, it) }
                    )
                    PhotoSlotControl(
                        slot = PhotoSlot.SIDE,
                        path = latest?.photoUriSide,
                        onPicked = { viewModel.setPhoto(PhotoSlot.SIDE, it) }
                    )
                    PhotoSlotControl(
                        slot = PhotoSlot.BACK,
                        path = latest?.photoUriBack,
                        onPicked = { viewModel.setPhoto(PhotoSlot.BACK, it) }
                    )
                }
            }
        }

        item {
            SectionCard(title = "Trends") {
                Column {
                    TrendChart(
                        title = "Weight",
                        points = measurements.mapNotNull { m -> m.weightKg?.let { m.date to it } },
                        unit = "kg"
                    )
                    Spacer(Modifier.height(24.dp))
                    TrendChart(
                        title = "Waist",
                        points = measurements.mapNotNull { m -> m.waistCm?.let { m.date to it } },
                        unit = "cm"
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoSlotControl(slot: PhotoSlot, path: String?, onPicked: (String) -> Unit) {
    val context = LocalContext.current
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            PhotoStore.importFrom(context, uri)?.let(onPicked)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null) onPicked(file.absolutePath)
        pendingCameraFile = null
    }

    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(slot.name.lowercase().replaceFirstChar { it.uppercase() })
        Spacer(Modifier.height(4.dp))

        var thumbnail by remember(path) {
            mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
        }
        LaunchedEffect(path) {
            thumbnail = path?.let { PhotoStore.loadThumbnail(it)?.asImageBitmap() }
        }

        val image = thumbnail
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "${slot.name} photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(4.dp))
        }

        OutlinedButton(
            onClick = {
                pickLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        ) { Text("Pick") }

        OutlinedButton(
            onClick = {
                val file = PhotoStore.newPhotoFile(context)
                pendingCameraFile = file
                cameraLauncher.launch(PhotoStore.shareUri(context, file))
            }
        ) { Text("Camera") }
    }
}
