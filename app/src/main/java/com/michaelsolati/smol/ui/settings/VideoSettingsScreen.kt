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
import com.michaelsolati.smol.data.model.VideoCodec
import com.michaelsolati.smol.data.model.VideoOutputFormat
import com.michaelsolati.smol.data.model.VideoResolution
import com.michaelsolati.smol.ui.components.SmolChipGroupSetting
import com.michaelsolati.smol.ui.components.SmolSliderSetting
import com.michaelsolati.smol.ui.components.SmolSwitchSetting

private val VIDEO_FILE_SIZE_OPTIONS = listOf(
    0L to "Off",
    8 * 1024 * 1024L to "8 MB",
    25 * 1024 * 1024L to "25 MB",
    50 * 1024 * 1024L to "50 MB",
    100 * 1024 * 1024L to "100 MB"
)

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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Resolution selection
        val resolutionOptions = listOf<VideoResolution?>(null) + VideoResolution.entries
        val resolutionLabelOptions = resolutionOptions.map { it to (it?.label ?: "Original") }
        SmolChipGroupSetting(
            title = "Resolution",
            subtitle = "Based on short edge — works for portrait and landscape",
            options = resolutionLabelOptions,
            selectedValue = settings.resolution,
            onSelected = { viewModel.updateVideoResolution(it) }
        )

        // Bitrate Mode and Slider
        val isOriginalBitrate = settings.bitrateMbps <= 0f
        val bitrateModeOptions = listOf(true to "Original", false to "Custom")
        
        SmolChipGroupSetting(
            title = "Bitrate Mode",
            subtitle = "Control video data rate and quality",
            options = bitrateModeOptions,
            selectedValue = isOriginalBitrate,
            onSelected = { isOriginal ->
                if (isOriginal) {
                    viewModel.updateVideoBitrate(0f)
                } else {
                    viewModel.updateVideoBitrate(4.0f) // Default to 4 Mbps when switching to custom
                }
            }
        )

        if (!isOriginalBitrate) {
            SmolSliderSetting(
                title = "Custom Bitrate",
                value = settings.bitrateMbps,
                onValueChange = { viewModel.updateVideoBitrate(it) },
                valueRange = 1f..20f,
                valueLabel = "${"%.1f".format(settings.bitrateMbps)} Mbps",
                steps = 18
            )
        }

        // Codec selection
        val codecOptions = listOf<VideoCodec?>(null) + VideoCodec.entries
        val codecLabelOptions = codecOptions.map { it to (it?.label ?: "Original") }
        SmolChipGroupSetting(
            title = "Codec",
            subtitle = "Encoder type for compression",
            options = codecLabelOptions,
            selectedValue = settings.codec,
            onSelected = { viewModel.updateVideoCodec(it) }
        )

        // Output format selection
        val outputFormatOptions = listOf<VideoOutputFormat?>(null) + VideoOutputFormat.entries
        val outputFormatLabelOptions = outputFormatOptions.map { it to (it?.label ?: "Original") }
        SmolChipGroupSetting(
            title = "Output Format",
            subtitle = "File container type",
            options = outputFormatLabelOptions,
            selectedValue = settings.outputFormat,
            onSelected = { viewModel.updateVideoOutputFormat(it) }
        )

        // Max file size
        SmolChipGroupSetting(
            title = "Max File Size",
            subtitle = "Adjusts bitrate to hit target (overrides custom bitrate)",
            options = VIDEO_FILE_SIZE_OPTIONS,
            selectedValue = settings.maxFileSizeBytes,
            onSelected = { viewModel.updateVideoMaxFileSize(it) }
        )

        // Strip metadata
        SmolSwitchSetting(
            title = "Strip Metadata",
            subtitle = "Remove location, date, and device info",
            checked = settings.stripMetadata,
            onCheckedChange = { viewModel.updateVideoStripMetadata(it) }
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}
