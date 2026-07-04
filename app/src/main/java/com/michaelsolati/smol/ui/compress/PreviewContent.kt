package com.michaelsolati.smol.ui.compress

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.michaelsolati.smol.data.model.ImageFormat
import com.michaelsolati.smol.data.model.MediaType
import com.michaelsolati.smol.data.model.VideoCodec
import com.michaelsolati.smol.data.model.VideoOutputFormat
import com.michaelsolati.smol.data.model.VideoResolution
import com.michaelsolati.smol.ui.components.SmolCard
import com.michaelsolati.smol.ui.components.SmolChipGroupSetting
import com.michaelsolati.smol.ui.components.SmolSliderSetting
import com.michaelsolati.smol.ui.components.SmolSwitchSetting
import com.michaelsolati.smol.util.FileUtil

private val IMAGE_FILE_SIZE_OPTIONS = listOf(
    0L to "Off",
    256 * 1024L to "256 KB",
    512 * 1024L to "512 KB",
    1024 * 1024L to "1 MB",
    2 * 1024 * 1024L to "2 MB",
    5 * 1024 * 1024L to "5 MB"
)

private val VIDEO_FILE_SIZE_OPTIONS = listOf(
    0L to "Off",
    5 * 1024 * 1024L to "5 MB",
    10 * 1024 * 1024L to "10 MB",
    25 * 1024 * 1024L to "25 MB",
    50 * 1024 * 1024L to "50 MB",
    100 * 1024 * 1024L to "100 MB"
)

@Composable
fun PreviewContent(
    state: CompressUiState.Preview,
    viewModel: CompressViewModel,
    onCompress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasImages = state.files.any { it.mediaType == MediaType.IMAGE }
    val hasVideos = state.files.any { it.mediaType == MediaType.VIDEO }
    val hasAudio = state.files.any { it.mediaType == MediaType.AUDIO }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Summary Header Card
        val totalOriginalSize = state.files.sumOf { it.size }
        SmolCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Original Total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FileUtil.formatFileSize(totalOriginalSize),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Estimated SMOL Size",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FileUtil.formatFileSize(state.estimatedTotalSize),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (totalOriginalSize > 0) {
                    val savingsPercent = ((totalOriginalSize - state.estimatedTotalSize) * 100 / totalOriginalSize).toInt()
                    if (savingsPercent > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Estimated Savings: ~$savingsPercent%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Horizontal file list
        Text(
            text = "Files to Compress (${state.files.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.files.forEach { file ->
                FileInfoCard(
                    file = file,
                    modifier = Modifier.width(280.dp)
                )
            }
        }

        // Settings sections
        if (hasImages) {
            SmolCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ImageSettingsSection(state, viewModel)
                }
            }
        }

        if (hasVideos) {
            SmolCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    VideoSettingsSection(state, viewModel)
                }
            }
        }

        if (hasAudio) {
            SmolCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    AudioSettingsSection(state, viewModel)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onCompress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Compress & Share",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ImageSettingsSection(
    state: CompressUiState.Preview,
    viewModel: CompressViewModel
) {
    val profile = state.imageProfileSettings
    val imageFiles = state.files.filter { it.mediaType == MediaType.IMAGE }
    val hasJpeg = imageFiles.any { it.mimeType.contains("jpeg") || it.mimeType.contains("jpg") }
    val hasPng = imageFiles.any { it.mimeType.contains("png") }
    val hasWebp = imageFiles.any { it.mimeType.contains("webp") }
    val hasGif = imageFiles.any { it.mimeType.contains("gif") }

    val settings = when {
        hasJpeg -> profile.jpeg
        hasPng -> profile.png
        hasWebp -> profile.webp
        hasGif -> profile.gif
        else -> profile.jpeg
    }

    Text(
        text = "Image Settings",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    // Quality
    SmolSliderSetting(
        title = "Quality",
        value = settings.quality.toFloat(),
        onValueChange = { newQuality ->
            val updated = profile.copy(
                jpeg = if (hasJpeg) profile.jpeg.copy(quality = newQuality.toInt()) else profile.jpeg,
                png = if (hasPng) profile.png.copy(quality = newQuality.toInt()) else profile.png,
                webp = if (hasWebp) profile.webp.copy(quality = newQuality.toInt()) else profile.webp,
                gif = if (hasGif) profile.gif.copy(quality = newQuality.toInt()) else profile.gif
            )
            viewModel.updateImageProfile(updated)
        },
        valueRange = 10f..100f,
        valueLabel = "${settings.quality}%",
        steps = 8
    )

    // Output format
    val formatOptions = listOf<ImageFormat?>(null) + ImageFormat.entries
    val formatLabelOptions = formatOptions.map { it to (it?.name ?: "Original") }
    SmolChipGroupSetting(
        title = "Output Format",
        options = formatLabelOptions,
        selectedValue = settings.format,
        onSelected = { format ->
            val updated = profile.copy(
                jpeg = if (hasJpeg) profile.jpeg.copy(format = format) else profile.jpeg,
                png = if (hasPng) profile.png.copy(format = format) else profile.png,
                webp = if (hasWebp) profile.webp.copy(format = format) else profile.webp,
                gif = if (hasGif) profile.gif.copy(format = format) else profile.gif
            )
            viewModel.updateImageProfile(updated)
        }
    )

    // Max resolution
    val resolutionOptions = listOf(0, 512, 1024, 2048, 4096)
    val resolutionLabelOptions = resolutionOptions.map { it to (if (it == 0) "Original" else "${it}px") }
    SmolChipGroupSetting(
        title = "Max Resolution",
        options = resolutionLabelOptions,
        selectedValue = settings.maxResolution,
        onSelected = { res ->
            val updated = profile.copy(
                jpeg = if (hasJpeg) profile.jpeg.copy(maxResolution = res) else profile.jpeg,
                png = if (hasPng) profile.png.copy(maxResolution = res) else profile.png,
                webp = if (hasWebp) profile.webp.copy(maxResolution = res) else profile.webp,
                gif = if (hasGif) profile.gif.copy(maxResolution = res) else profile.gif
            )
            viewModel.updateImageProfile(updated)
        }
    )

    // Max file size
    SmolChipGroupSetting(
        title = "Max File Size",
        options = IMAGE_FILE_SIZE_OPTIONS,
        selectedValue = settings.maxFileSizeBytes,
        onSelected = { bytes ->
            val updated = profile.copy(
                jpeg = if (hasJpeg) profile.jpeg.copy(maxFileSizeBytes = bytes) else profile.jpeg,
                png = if (hasPng) profile.png.copy(maxFileSizeBytes = bytes) else profile.png,
                webp = if (hasWebp) profile.webp.copy(maxFileSizeBytes = bytes) else profile.webp,
                gif = if (hasGif) profile.gif.copy(maxFileSizeBytes = bytes) else profile.gif
            )
            viewModel.updateImageProfile(updated)
        }
    )

    // Strip metadata
    SmolSwitchSetting(
        title = "Strip Metadata",
        checked = settings.stripMetadata,
        onCheckedChange = { strip ->
            val updated = profile.copy(
                jpeg = if (hasJpeg) profile.jpeg.copy(stripMetadata = strip) else profile.jpeg,
                png = if (hasPng) profile.png.copy(stripMetadata = strip) else profile.png,
                webp = if (hasWebp) profile.webp.copy(stripMetadata = strip) else profile.webp,
                gif = if (hasGif) profile.gif.copy(stripMetadata = strip) else profile.gif
            )
            viewModel.updateImageProfile(updated)
        }
    )
}

@Composable
private fun VideoSettingsSection(
    state: CompressUiState.Preview,
    viewModel: CompressViewModel
) {
    val settings = state.videoSettings

    Text(
        text = "Video Settings",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    // Resolution
    val resolutionOptions = listOf<VideoResolution?>(null) + VideoResolution.entries
    val resolutionLabelOptions = resolutionOptions.map { it to (it?.label ?: "Original") }
    SmolChipGroupSetting(
        title = "Resolution",
        options = resolutionLabelOptions,
        selectedValue = settings.resolution,
        onSelected = { viewModel.updateVideoSettings(settings.copy(resolution = it)) }
    )

    // Bitrate Mode and Slider
    val isOriginalBitrate = settings.bitrateMbps <= 0f
    val bitrateModeOptions = listOf(true to "Original", false to "Custom")
    SmolChipGroupSetting(
        title = "Bitrate Mode",
        options = bitrateModeOptions,
        selectedValue = isOriginalBitrate,
        onSelected = { isOriginal ->
            if (isOriginal) {
                viewModel.updateVideoSettings(settings.copy(bitrateMbps = 0f))
            } else {
                viewModel.updateVideoSettings(settings.copy(bitrateMbps = 4.0f))
            }
        }
    )

    if (!isOriginalBitrate) {
        SmolSliderSetting(
            title = "Custom Bitrate",
            value = settings.bitrateMbps,
            onValueChange = { viewModel.updateVideoSettings(settings.copy(bitrateMbps = it)) },
            valueRange = 0.5f..20f,
            valueLabel = "${"%.1f".format(settings.bitrateMbps)} Mbps",
            steps = 38
        )
    }

    // Codec
    val codecOptions = listOf<VideoCodec?>(null) + VideoCodec.entries
    val codecLabelOptions = codecOptions.map { it to (it?.label ?: "Original") }
    SmolChipGroupSetting(
        title = "Codec",
        options = codecLabelOptions,
        selectedValue = settings.codec,
        onSelected = { viewModel.updateVideoSettings(settings.copy(codec = it)) }
    )

    // Output format
    val outputFormatOptions = listOf<VideoOutputFormat?>(null) + VideoOutputFormat.entries
    val outputFormatLabelOptions = outputFormatOptions.map { it to (it?.label ?: "Original") }
    SmolChipGroupSetting(
        title = "Output Format",
        options = outputFormatLabelOptions,
        selectedValue = settings.outputFormat,
        onSelected = { viewModel.updateVideoSettings(settings.copy(outputFormat = it)) }
    )

    // Max file size
    SmolChipGroupSetting(
        title = "Max File Size",
        options = VIDEO_FILE_SIZE_OPTIONS,
        selectedValue = settings.maxFileSizeBytes,
        onSelected = { viewModel.updateVideoSettings(settings.copy(maxFileSizeBytes = it)) }
    )

    // Strip metadata
    SmolSwitchSetting(
        title = "Strip Metadata",
        checked = settings.stripMetadata,
        onCheckedChange = { viewModel.updateVideoSettings(settings.copy(stripMetadata = it)) }
    )
}

@Composable
private fun AudioSettingsSection(
    state: CompressUiState.Preview,
    viewModel: CompressViewModel
) {
    val settings = state.audioSettings

    Text(
        text = "Audio Settings",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    // Bitrate Mode and Slider
    val isOriginalBitrate = settings.bitrate <= 0
    val bitrateModeOptions = listOf(true to "Original", false to "Custom")
    SmolChipGroupSetting(
        title = "Bitrate Mode",
        options = bitrateModeOptions,
        selectedValue = isOriginalBitrate,
        onSelected = { isOriginal ->
            if (isOriginal) {
                viewModel.updateAudioSettings(settings.copy(bitrate = 0))
            } else {
                viewModel.updateAudioSettings(settings.copy(bitrate = 128))
            }
        }
    )

    if (!isOriginalBitrate) {
        SmolSliderSetting(
            title = "Custom Bitrate",
            value = settings.bitrate.toFloat(),
            onValueChange = { viewModel.updateAudioSettings(settings.copy(bitrate = it.toInt())) },
            valueRange = 32f..320f,
            valueLabel = "${settings.bitrate} kbps",
            steps = 8
        )
    }

    // Format
    val formatOptions = listOf<com.michaelsolati.smol.data.model.AudioFormat?>(null) + com.michaelsolati.smol.data.model.AudioFormat.entries
    val formatLabelOptions = formatOptions.map { it to (it?.label ?: "Original") }
    SmolChipGroupSetting(
        title = "Format",
        options = formatLabelOptions,
        selectedValue = settings.format,
        onSelected = { viewModel.updateAudioSettings(settings.copy(format = it)) }
    )

    // Strip metadata
    SmolSwitchSetting(
        title = "Strip Metadata",
        checked = settings.stripMetadata,
        onCheckedChange = { viewModel.updateAudioSettings(settings.copy(stripMetadata = it)) }
    )
}
