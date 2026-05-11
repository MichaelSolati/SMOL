package com.michaelsolati.smol.ui.compress

import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.michaelsolati.smol.data.model.ImageFormat
import com.michaelsolati.smol.data.model.MediaType
import com.michaelsolati.smol.data.model.VideoCodec
import com.michaelsolati.smol.data.model.VideoOutputFormat
import com.michaelsolati.smol.data.model.VideoResolution
import com.michaelsolati.smol.util.FileUtil
import com.michaelsolati.smol.util.ShareUtil

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressScreen(
    viewModel: CompressViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SMOL") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = uiState) {
                is CompressUiState.Idle, is CompressUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is CompressUiState.Preview -> {
                    PreviewContent(
                        state = state,
                        viewModel = viewModel,
                        onCompress = { viewModel.compress() }
                    )
                }

                is CompressUiState.Compressing -> {
                    CompressingContent(state = state)
                }

                is CompressUiState.Done -> {
                    DoneContent(
                        state = state,
                        onShare = { results ->
                            val uris = results.map { it.compressedUri }
                            val mimeType = results.first().let { result ->
                                when (result.mediaType) {
                                    MediaType.IMAGE -> "image/*"
                                    MediaType.VIDEO -> "video/*"
                                    MediaType.AUDIO -> "audio/*"
                                }
                            }
                            val intent = ShareUtil.createShareIntent(context, uris, mimeType)
                            context.startActivity(Intent.createChooser(intent, "Share compressed file"))
                        },
                        onSave = { results ->
                            results.forEach { result ->
                                val mimeType = when (result.mediaType) {
                                    MediaType.IMAGE -> "image/*"
                                    MediaType.VIDEO -> "video/*"
                                    MediaType.AUDIO -> "audio/*"
                                }
                                val fileName = "smol_${System.currentTimeMillis()}"
                                ShareUtil.saveToDownloads(context, result.compressedUri, fileName, mimeType)
                            }
                            android.widget.Toast.makeText(context, "Saved to Downloads/SMOL", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                is CompressUiState.Error -> {
                    ErrorContent(
                        state = state,
                        onRetry = { viewModel.retry() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewContent(
    state: CompressUiState.Preview,
    viewModel: CompressViewModel,
    onCompress: () -> Unit
) {
    val hasImages = state.files.any { it.mediaType == MediaType.IMAGE }
    val hasVideos = state.files.any { it.mediaType == MediaType.VIDEO }
    val hasAudio = state.files.any { it.mediaType == MediaType.AUDIO }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary header
        val totalOriginalSize = state.files.sumOf { it.size }
        Text(
            text = "Original: ${FileUtil.formatFileSize(totalOriginalSize)}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Estimated: ${FileUtil.formatFileSize(state.estimatedTotalSize)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )

        if (totalOriginalSize > 0) {
            val savingsPercent = ((totalOriginalSize - state.estimatedTotalSize) * 100 / totalOriginalSize).toInt()
            if (savingsPercent > 0) {
                Text(
                    text = "Savings: ~$savingsPercent%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // File list
        state.files.forEach { file ->
            FileInfoCard(file)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Image settings (if images present)
        if (hasImages) {
            ImageSettingsSection(state, viewModel)
        }

        // Video settings (if videos present)
        if (hasVideos) {
            VideoSettingsSection(state, viewModel)
        }

        // Audio settings (if audio present)
        if (hasAudio) {
            AudioSettingsSection(state, viewModel)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onCompress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Compress & Share")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageSettingsSection(
    state: CompressUiState.Preview,
    viewModel: CompressViewModel
) {
    val profile = state.imageProfileSettings
    // Determine which formats are in the file set
    val imageFiles = state.files.filter { it.mediaType == MediaType.IMAGE }
    val hasJpeg = imageFiles.any { it.mimeType.contains("jpeg") || it.mimeType.contains("jpg") }
    val hasPng = imageFiles.any { it.mimeType.contains("png") }
    val hasWebp = imageFiles.any { it.mimeType.contains("webp") }
    val hasGif = imageFiles.any { it.mimeType.contains("gif") }

    // Show a combined view — use the first relevant format's settings
    val settings = when {
        hasJpeg -> profile.jpeg
        hasPng -> profile.png
        hasWebp -> profile.webp
        hasGif -> profile.gif
        else -> profile.jpeg
    }

    Text(
        text = "Image Settings",
        style = MaterialTheme.typography.titleMedium
    )

    // Quality
    Text(
        text = "Quality: ${settings.quality}%",
        style = MaterialTheme.typography.bodyMedium
    )
    Slider(
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
        steps = 8
    )

    // Output format
    Text(text = "Output Format", style = MaterialTheme.typography.bodyMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = settings.format == null,
            onClick = {
                val updated = profile.copy(
                    jpeg = if (hasJpeg) profile.jpeg.copy(format = null) else profile.jpeg,
                    png = if (hasPng) profile.png.copy(format = null) else profile.png,
                    webp = if (hasWebp) profile.webp.copy(format = null) else profile.webp,
                    gif = if (hasGif) profile.gif.copy(format = null) else profile.gif
                )
                viewModel.updateImageProfile(updated)
            },
            label = { Text("Original") }
        )
        ImageFormat.entries.forEach { format ->
            FilterChip(
                selected = settings.format == format,
                onClick = {
                    val updated = profile.copy(
                        jpeg = if (hasJpeg) profile.jpeg.copy(format = format) else profile.jpeg,
                        png = if (hasPng) profile.png.copy(format = format) else profile.png,
                        webp = if (hasWebp) profile.webp.copy(format = format) else profile.webp,
                        gif = if (hasGif) profile.gif.copy(format = format) else profile.gif
                    )
                    viewModel.updateImageProfile(updated)
                },
                label = { Text(format.name) }
            )
        }
    }

    // Max resolution
    Text(text = "Max Resolution", style = MaterialTheme.typography.bodyMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = settings.maxResolution == 0,
            onClick = {
                val updated = profile.copy(
                    jpeg = if (hasJpeg) profile.jpeg.copy(maxResolution = 0) else profile.jpeg,
                    png = if (hasPng) profile.png.copy(maxResolution = 0) else profile.png,
                    webp = if (hasWebp) profile.webp.copy(maxResolution = 0) else profile.webp,
                    gif = if (hasGif) profile.gif.copy(maxResolution = 0) else profile.gif
                )
                viewModel.updateImageProfile(updated)
            },
            label = { Text("Original") }
        )
        listOf(512, 1024, 2048, 4096).forEach { res ->
            FilterChip(
                selected = settings.maxResolution == res,
                onClick = {
                    val updated = profile.copy(
                        jpeg = if (hasJpeg) profile.jpeg.copy(maxResolution = res) else profile.jpeg,
                        png = if (hasPng) profile.png.copy(maxResolution = res) else profile.png,
                        webp = if (hasWebp) profile.webp.copy(maxResolution = res) else profile.webp,
                        gif = if (hasGif) profile.gif.copy(maxResolution = res) else profile.gif
                    )
                    viewModel.updateImageProfile(updated)
                },
                label = { Text("${res}px") }
            )
        }
    }

    // Max file size
    Text(text = "Max File Size", style = MaterialTheme.typography.bodyMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IMAGE_FILE_SIZE_OPTIONS.forEach { (bytes, label) ->
            FilterChip(
                selected = settings.maxFileSizeBytes == bytes,
                onClick = {
                    val updated = profile.copy(
                        jpeg = if (hasJpeg) profile.jpeg.copy(maxFileSizeBytes = bytes) else profile.jpeg,
                        png = if (hasPng) profile.png.copy(maxFileSizeBytes = bytes) else profile.png,
                        webp = if (hasWebp) profile.webp.copy(maxFileSizeBytes = bytes) else profile.webp,
                        gif = if (hasGif) profile.gif.copy(maxFileSizeBytes = bytes) else profile.gif
                    )
                    viewModel.updateImageProfile(updated)
                },
                label = { Text(label) }
            )
        }
    }

    // Strip metadata
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Strip Metadata", style = MaterialTheme.typography.bodyMedium)
        Switch(
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoSettingsSection(
    state: CompressUiState.Preview,
    viewModel: CompressViewModel
) {
    val settings = state.videoSettings

    Text(
        text = "Video Settings",
        style = MaterialTheme.typography.titleMedium
    )

    // Resolution
    Text(text = "Resolution", style = MaterialTheme.typography.bodyMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = settings.resolution == null,
            onClick = { viewModel.updateVideoSettings(settings.copy(resolution = null)) },
            label = { Text("Original") }
        )
        VideoResolution.entries.forEach { res ->
            FilterChip(
                selected = settings.resolution == res,
                onClick = { viewModel.updateVideoSettings(settings.copy(resolution = res)) },
                label = { Text(res.label) }
            )
        }
    }

    // Bitrate
    val bitrateLabel = if (settings.bitrateMbps <= 0f) "Original" else "${"%.1f".format(settings.bitrateMbps)} Mbps"
    Text(
        text = "Bitrate: $bitrateLabel",
        style = MaterialTheme.typography.bodyMedium
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = settings.bitrateMbps <= 0f,
            onClick = { viewModel.updateVideoSettings(settings.copy(bitrateMbps = 0f)) },
            label = { Text("Original") }
        )
    }
    Slider(
        value = if (settings.bitrateMbps > 0f) settings.bitrateMbps else 4f,
        onValueChange = { viewModel.updateVideoSettings(settings.copy(bitrateMbps = it)) },
        valueRange = 0.5f..20f,
        steps = 38
    )

    // Codec
    Text(text = "Codec", style = MaterialTheme.typography.bodyMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = settings.codec == null,
            onClick = { viewModel.updateVideoSettings(settings.copy(codec = null)) },
            label = { Text("Original") }
        )
        VideoCodec.entries.forEach { codec ->
            FilterChip(
                selected = settings.codec == codec,
                onClick = { viewModel.updateVideoSettings(settings.copy(codec = codec)) },
                label = { Text(codec.label) }
            )
        }
    }

    // Output format
    Text(text = "Output Format", style = MaterialTheme.typography.bodyMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = settings.outputFormat == null,
            onClick = { viewModel.updateVideoSettings(settings.copy(outputFormat = null)) },
            label = { Text("Original") }
        )
        VideoOutputFormat.entries.forEach { format ->
            FilterChip(
                selected = settings.outputFormat == format,
                onClick = { viewModel.updateVideoSettings(settings.copy(outputFormat = format)) },
                label = { Text(format.label) }
            )
        }
    }

    // Max file size
    Text(text = "Max File Size", style = MaterialTheme.typography.bodyMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        VIDEO_FILE_SIZE_OPTIONS.forEach { (bytes, label) ->
            FilterChip(
                selected = settings.maxFileSizeBytes == bytes,
                onClick = { viewModel.updateVideoSettings(settings.copy(maxFileSizeBytes = bytes)) },
                label = { Text(label) }
            )
        }
    }

    // Strip metadata
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Strip Metadata", style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = settings.stripMetadata,
            onCheckedChange = { viewModel.updateVideoSettings(settings.copy(stripMetadata = it)) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioSettingsSection(
    state: CompressUiState.Preview,
    viewModel: CompressViewModel
) {
    val settings = state.audioSettings

    Text(
        text = "Audio Settings",
        style = MaterialTheme.typography.titleMedium
    )

    // Bitrate
    val audioBitrateLabel = if (settings.bitrate <= 0) "Original" else "${settings.bitrate} kbps"
    Text(
        text = "Bitrate: $audioBitrateLabel",
        style = MaterialTheme.typography.bodyMedium
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = settings.bitrate <= 0,
            onClick = { viewModel.updateAudioSettings(settings.copy(bitrate = 0)) },
            label = { Text("Original") }
        )
    }
    Slider(
        value = if (settings.bitrate > 0) settings.bitrate.toFloat() else 128f,
        onValueChange = { viewModel.updateAudioSettings(settings.copy(bitrate = it.toInt())) },
        valueRange = 32f..320f,
        steps = 8
    )

    // Format
    Text(text = "Format", style = MaterialTheme.typography.bodyMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = settings.format == null,
            onClick = { viewModel.updateAudioSettings(settings.copy(format = null)) },
            label = { Text("Original") }
        )
        com.michaelsolati.smol.data.model.AudioFormat.entries.forEach { format ->
            FilterChip(
                selected = settings.format == format,
                onClick = { viewModel.updateAudioSettings(settings.copy(format = format)) },
                label = { Text(format.label) }
            )
        }
    }

    // Strip metadata
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Strip Metadata", style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = settings.stripMetadata,
            onCheckedChange = { viewModel.updateAudioSettings(settings.copy(stripMetadata = it)) }
        )
    }
}

@Composable
private fun FileInfoCard(file: SharedFileInfo) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (file.mediaType) {
                    MediaType.IMAGE -> Icons.Default.Image
                    MediaType.VIDEO -> Icons.Default.Videocam
                    MediaType.AUDIO -> Icons.Default.AudioFile
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = FileUtil.formatFileSize(file.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompressingContent(state: CompressUiState.Compressing) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = "Compressing file ${state.currentFile} of ${state.totalFiles}...",
            style = MaterialTheme.typography.bodyLarge
        )
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DoneContent(
    state: CompressUiState.Done,
    onShare: (List<com.michaelsolati.smol.data.model.CompressionResult>) -> Unit,
    onSave: (List<com.michaelsolati.smol.data.model.CompressionResult>) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Compression Complete!",
            style = MaterialTheme.typography.headlineSmall
        )

        val totalOriginal = state.results.sumOf { it.originalSize }
        val totalCompressed = state.results.sumOf { it.compressedSize }
        val savingsPercent = if (totalOriginal > 0) {
            ((totalOriginal - totalCompressed) * 100 / totalOriginal).toInt()
        } else 0

        Text(
            text = "${FileUtil.formatFileSize(totalOriginal)} → ${FileUtil.formatFileSize(totalCompressed)}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = if (savingsPercent > 0) "Saved $savingsPercent%" else "No size reduction",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onShare(state.results) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Share")
        }

        OutlinedButton(
            onClick = { onSave(state.results) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save to Device")
        }
    }
}

@Composable
private fun ErrorContent(
    state: CompressUiState.Error,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = "Compression Failed",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}
