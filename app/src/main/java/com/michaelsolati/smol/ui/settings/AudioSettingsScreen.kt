package com.michaelsolati.smol.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.michaelsolati.smol.data.model.AudioFormat
import com.michaelsolati.smol.ui.components.SmolChipGroupSetting
import com.michaelsolati.smol.ui.components.SmolSliderSetting
import com.michaelsolati.smol.ui.components.SmolSwitchSetting

private val AUDIO_FILE_SIZE_OPTIONS = listOf(
    0L to "Off",
    1 * 1024 * 1024L to "1 MB",
    5 * 1024 * 1024L to "5 MB",
    10 * 1024 * 1024L to "10 MB",
    25 * 1024 * 1024L to "25 MB"
)

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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Bitrate Mode and Slider
        val isOriginalBitrate = settings.bitrate <= 0
        val bitrateModeOptions = listOf(true to "Original", false to "Custom")
        
        SmolChipGroupSetting(
            title = "Bitrate Mode",
            subtitle = "Control audio sound quality and file size",
            options = bitrateModeOptions,
            selectedValue = isOriginalBitrate,
            onSelected = { isOriginal ->
                if (isOriginal) {
                    viewModel.updateAudioBitrate(0)
                } else {
                    viewModel.updateAudioBitrate(128) // Default to 128 kbps
                }
            }
        )

        if (!isOriginalBitrate) {
            SmolSliderSetting(
                title = "Custom Bitrate",
                value = settings.bitrate.toFloat(),
                onValueChange = { viewModel.updateAudioBitrate(it.toInt()) },
                valueRange = 64f..320f,
                valueLabel = "${settings.bitrate} kbps",
                steps = 4
            )
        }

        // Format selection
        val formatOptions = listOf<AudioFormat?>(null) + AudioFormat.entries
        val formatLabelOptions = formatOptions.map { it to (it?.label ?: "Original") }
        SmolChipGroupSetting(
            title = "Format",
            subtitle = "File container type for audio",
            options = formatLabelOptions,
            selectedValue = settings.format,
            onSelected = { viewModel.updateAudioFormat(it) }
        )

        // Max file size
        SmolChipGroupSetting(
            title = "Max File Size",
            subtitle = "Adjusts bitrate to hit target size limit",
            options = AUDIO_FILE_SIZE_OPTIONS,
            selectedValue = settings.maxFileSizeBytes,
            onSelected = { viewModel.updateAudioMaxFileSize(it) }
        )

        // Strip metadata
        SmolSwitchSetting(
            title = "Strip Metadata",
            subtitle = "Remove album art, tags, and comments",
            checked = settings.stripMetadata,
            onCheckedChange = { viewModel.updateAudioStripMetadata(it) }
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}
