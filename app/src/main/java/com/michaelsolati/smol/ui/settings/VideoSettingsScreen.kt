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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.michaelsolati.smol.data.model.VideoCodec
import com.michaelsolati.smol.data.model.VideoOutputFormat
import com.michaelsolati.smol.data.model.VideoResolution

private val VIDEO_FILE_SIZE_OPTIONS = listOf(
    0L to "Off",
    8 * 1024 * 1024L to "8 MB",
    25 * 1024 * 1024L to "25 MB",
    50 * 1024 * 1024L to "50 MB",
    100 * 1024 * 1024L to "100 MB"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoSettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.videoSettings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 72.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Resolution selection
        Column {
            val resLabel = settings.resolution?.label ?: "Original"
            Text(
                text = "Resolution: $resLabel",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Based on short edge — works for portrait and landscape",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = settings.resolution == null,
                    onClick = { viewModel.updateVideoResolution(null) },
                    label = { Text("Original") }
                )
                VideoResolution.entries.forEach { resolution ->
                    FilterChip(
                        selected = settings.resolution == resolution,
                        onClick = { viewModel.updateVideoResolution(resolution) },
                        label = { Text(resolution.label) }
                    )
                }
            }
        }

        // Bitrate slider
        Column {
            val bitrateLabel = if (settings.bitrateMbps <= 0f) "Original" else "${"%.1f".format(settings.bitrateMbps)} Mbps"
            Text(
                text = "Bitrate: $bitrateLabel",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = settings.bitrateMbps <= 0f,
                    onClick = { viewModel.updateVideoBitrate(0f) },
                    label = { Text("Original") }
                )
            }
            if (settings.bitrateMbps > 0f) {
                Slider(
                    value = settings.bitrateMbps,
                    onValueChange = { viewModel.updateVideoBitrate(it) },
                    valueRange = 1f..20f,
                    steps = 18
                )
            } else {
                Slider(
                    value = 4f,
                    onValueChange = { viewModel.updateVideoBitrate(it) },
                    valueRange = 1f..20f,
                    steps = 18
                )
            }
        }

        // Codec selection
        Column {
            Text(
                text = "Codec",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = settings.codec == null,
                    onClick = { viewModel.updateVideoCodec(null) },
                    label = { Text("Original") }
                )
                VideoCodec.entries.forEach { codec ->
                    FilterChip(
                        selected = settings.codec == codec,
                        onClick = { viewModel.updateVideoCodec(codec) },
                        label = { Text(codec.label) }
                    )
                }
            }
        }

        // Output format selection
        Column {
            Text(
                text = "Output Format",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = settings.outputFormat == null,
                    onClick = { viewModel.updateVideoOutputFormat(null) },
                    label = { Text("Original") }
                )
                VideoOutputFormat.entries.forEach { format ->
                    FilterChip(
                        selected = settings.outputFormat == format,
                        onClick = { viewModel.updateVideoOutputFormat(format) },
                        label = { Text(format.label) }
                    )
                }
            }
        }

        // Max file size
        Column {
            val currentLabel = VIDEO_FILE_SIZE_OPTIONS.find { it.first == settings.maxFileSizeBytes }?.second
                ?: "Custom"
            Text(
                text = "Max File Size: $currentLabel",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Adjusts bitrate to hit target (overrides bitrate setting)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VIDEO_FILE_SIZE_OPTIONS.forEach { (bytes, label) ->
                    FilterChip(
                        selected = settings.maxFileSizeBytes == bytes,
                        onClick = { viewModel.updateVideoMaxFileSize(bytes) },
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
                    text = "Remove location, date, device info",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.stripMetadata,
                onCheckedChange = { viewModel.updateVideoStripMetadata(it) }
            )
        }
    }
}
