package com.michaelsolati.smol.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.michaelsolati.smol.data.model.ImageCompressionSettings
import com.michaelsolati.smol.data.model.ImageFormat
import com.michaelsolati.smol.ui.components.SmolChipGroupSetting
import com.michaelsolati.smol.ui.components.SmolSliderSetting
import com.michaelsolati.smol.ui.components.SmolSwitchSetting

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

@Composable
fun ImageSettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val profileSettings by viewModel.imageProfileSettings.collectAsState()
    val pagerState = rememberPagerState(pageCount = { FORMAT_TABS.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Styled TabRow linked to PagerState
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            FORMAT_TABS.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val formatKey = FORMAT_KEYS[page]
            val settings = when (page) {
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
}

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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Quality Slider
        SmolSliderSetting(
            title = "Quality",
            value = settings.quality.toFloat(),
            onValueChange = { viewModel.updateImageQuality(formatKey, it.toInt()) },
            valueRange = 10f..100f,
            valueLabel = "${settings.quality}%",
            steps = 8
        )

        // Output format selection
        val formatOptions = listOf<ImageFormat?>(null) + ImageFormat.entries
        val formatLabelOptions = formatOptions.map { it to (it?.name ?: "Original") }
        SmolChipGroupSetting(
            title = "Output Format",
            subtitle = "Format to encode the output as",
            options = formatLabelOptions,
            selectedValue = settings.format,
            onSelected = { viewModel.updateImageFormat(formatKey, it) }
        )

        // Max resolution
        val resolutionOptions = listOf(0, 512, 1024, 2048, 4096)
        val resLabel = if (settings.maxResolution <= 0) "Original" else "${settings.maxResolution}px"
        val resolutionLabelOptions = resolutionOptions.map { it to (if (it == 0) "Original" else "${it}px") }
        SmolChipGroupSetting(
            title = "Max Resolution",
            subtitle = "Longest edge will be scaled to this size (current: $resLabel)",
            options = resolutionLabelOptions,
            selectedValue = settings.maxResolution,
            onSelected = { viewModel.updateImageMaxResolution(formatKey, it) }
        )

        // Target file size
        SmolChipGroupSetting(
            title = "Max File Size",
            subtitle = "Iteratively reduces quality to hit target size",
            options = FILE_SIZE_OPTIONS,
            selectedValue = settings.maxFileSizeBytes,
            onSelected = { viewModel.updateImageMaxFileSize(formatKey, it) }
        )

        // Strip metadata
        SmolSwitchSetting(
            title = "Strip Metadata",
            subtitle = "Remove EXIF data (GPS, camera info, etc.)",
            checked = settings.stripMetadata,
            onCheckedChange = { viewModel.updateImageStripMetadata(formatKey, it) }
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}
