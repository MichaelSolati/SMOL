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
import com.michaelsolati.smol.data.model.AudioFormat

private val AUDIO_FILE_SIZE_OPTIONS = listOf(
    0L to "Off",
    1 * 1024 * 1024L to "1 MB",
    5 * 1024 * 1024L to "5 MB",
    10 * 1024 * 1024L to "10 MB",
    25 * 1024 * 1024L to "25 MB"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioSettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.audioSettings.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 72.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Bitrate slider
        Column {
            val bitrateLabel = if (settings.bitrate <= 0) "Original" else "${settings.bitrate} kbps"
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
                    selected = settings.bitrate <= 0,
                    onClick = { viewModel.updateAudioBitrate(0) },
                    label = { Text("Original") }
                )
            }
            if (settings.bitrate > 0) {
                Slider(
                    value = settings.bitrate.toFloat(),
                    onValueChange = { viewModel.updateAudioBitrate(it.toInt()) },
                    valueRange = 64f..320f,
                    steps = 4
                )
            } else {
                Slider(
                    value = 128f,
                    onValueChange = { viewModel.updateAudioBitrate(it.toInt()) },
                    valueRange = 64f..320f,
                    steps = 4
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("64 kbps", style = MaterialTheme.typography.bodySmall)
                Text("320 kbps", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Format selection
        Column {
            Text(
                text = "Format",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = settings.format == null,
                    onClick = { viewModel.updateAudioFormat(null) },
                    label = { Text("Original") }
                )
                AudioFormat.entries.forEach { format ->
                    FilterChip(
                        selected = settings.format == format,
                        onClick = { viewModel.updateAudioFormat(format) },
                        label = { Text(format.label) }
                    )
                }
            }
        }

        // Max file size
        Column {
            val currentLabel = AUDIO_FILE_SIZE_OPTIONS.find { it.first == settings.maxFileSizeBytes }?.second
                ?: "Custom"
            Text(
                text = "Max File Size: $currentLabel",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Adjusts bitrate to hit target",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AUDIO_FILE_SIZE_OPTIONS.forEach { (bytes, label) ->
                    FilterChip(
                        selected = settings.maxFileSizeBytes == bytes,
                        onClick = { viewModel.updateAudioMaxFileSize(bytes) },
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
                    text = "Remove album art, tags, comments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.stripMetadata,
                onCheckedChange = { viewModel.updateAudioStripMetadata(it) }
            )
        }
    }
}
