package com.michaelsolati.smol.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.michaelsolati.smol.data.model.ImageCompressionSettings
import com.michaelsolati.smol.data.model.ImageFormat

private val FILE_SIZE_OPTIONS = listOf(
    0L to "Off",
    256 * 1024L to "256 KB",
    512 * 1024L to "512 KB",
    1024 * 1024L to "1 MB",
    2 * 1024 * 1024L to "2 MB",
    5 * 1024 * 1024L to "5 MB"
)

private val FORMAT_TABS = listOf("JPEG", "PNG", "WEBP", "GIF")
private val FORMAT_KEYS = listOf("jpeg", "png", "webp", "gif")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageSettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val profileSettings by viewModel.imageProfileSettings.collectAsState()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Format tabs
        TabRow(selectedTabIndex = selectedTabIndex) {
            FORMAT_TABS.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        val formatKey = FORMAT_KEYS[selectedTabIndex]
        val settings = when (selectedTabIndex) {
            0 -> profileSettings.jpeg
            1 -> profileSettings.png
            2 -> profileSettings.webp
            3 -> profileSettings.gif
            else -> profileSettings.jpeg
        }

        ImageFormatSettingsPanel(
            settings = settings,
            formatKey = formatKey,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageFormatSettingsPanel(
    settings: ImageCompressionSettings,
    formatKey: String,
    viewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 72.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Quality slider
        Column {
            Text(
                text = "Quality: ${settings.quality}%",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = settings.quality.toFloat(),
                onValueChange = { viewModel.updateImageQuality(formatKey, it.toInt()) },
                valueRange = 10f..100f,
                steps = 8
            )
        }

        // Output format selection
        Column {
            Text(
                text = "Output Format",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Format to encode the output as",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = settings.format == null,
                    onClick = { viewModel.updateImageFormat(formatKey, null) },
                    label = { Text("Original") }
                )
                ImageFormat.entries.forEach { format ->
                    FilterChip(
                        selected = settings.format == format,
                        onClick = { viewModel.updateImageFormat(formatKey, format) },
                        label = { Text(format.name) }
                    )
                }
            }
        }

        // Max resolution
        Column {
            val resLabel = if (settings.maxResolution <= 0) "Original" else "${settings.maxResolution}px"
            Text(
                text = "Max Resolution: $resLabel",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Longest edge will be scaled to this size",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = settings.maxResolution == 0,
                    onClick = { viewModel.updateImageMaxResolution(formatKey, 0) },
                    label = { Text("Original") }
                )
                val resolutions = listOf(512, 1024, 2048, 4096)
                resolutions.forEach { res ->
                    FilterChip(
                        selected = settings.maxResolution == res,
                        onClick = { viewModel.updateImageMaxResolution(formatKey, res) },
                        label = { Text("${res}px") }
                    )
                }
            }
        }

        // Target file size
        Column {
            val currentLabel = FILE_SIZE_OPTIONS.find { it.first == settings.maxFileSizeBytes }?.second
                ?: "Custom"
            Text(
                text = "Max File Size: $currentLabel",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Iteratively reduces quality to hit target size",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FILE_SIZE_OPTIONS.forEach { (bytes, label) ->
                    FilterChip(
                        selected = settings.maxFileSizeBytes == bytes,
                        onClick = { viewModel.updateImageMaxFileSize(formatKey, bytes) },
                        label = { Text(label) }
                    )
                }
            }
        }

        // Strip metadata
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Strip Metadata",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Remove EXIF data (GPS, camera info, etc.)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.stripMetadata,
                onCheckedChange = { viewModel.updateImageStripMetadata(formatKey, it) }
            )
        }
    }
}
