package com.michaelsolati.smol.ui.compress

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelsolati.smol.data.CompressionPreferences
import com.michaelsolati.smol.data.model.AudioCompressionSettings
import com.michaelsolati.smol.data.model.CompressionResult
import com.michaelsolati.smol.data.model.ImageCompressionSettings
import com.michaelsolati.smol.data.model.ImageProfileSettings
import com.michaelsolati.smol.data.model.MediaType
import com.michaelsolati.smol.data.model.VideoCompressionSettings
import com.michaelsolati.smol.service.MediaCompressor
import com.michaelsolati.smol.util.FileUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SharedFileInfo(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val mediaType: MediaType
)

sealed class CompressUiState {
    data object Idle : CompressUiState()
    data object Loading : CompressUiState()
    data class Preview(
        val files: List<SharedFileInfo>,
        val estimatedTotalSize: Long,
        val imageProfileSettings: ImageProfileSettings,
        val videoSettings: VideoCompressionSettings,
        val audioSettings: AudioCompressionSettings
    ) : CompressUiState()
    data class Compressing(val progress: Float, val currentFile: Int, val totalFiles: Int) : CompressUiState()
    data class Done(val results: List<CompressionResult>) : CompressUiState()
    data class Error(val message: String) : CompressUiState()
}

@HiltViewModel
class CompressViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaCompressor: MediaCompressor,
    private val preferences: CompressionPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<CompressUiState>(CompressUiState.Idle)
    val uiState: StateFlow<CompressUiState> = _uiState

    private var currentFiles: List<SharedFileInfo> = emptyList()
    private var imageProfileSettings = ImageProfileSettings()
    private var videoSettings = VideoCompressionSettings()
    private var audioSettings = AudioCompressionSettings()

    fun loadSharedFiles(uris: List<Uri>, autoCompress: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = CompressUiState.Loading

            // Load default settings
            imageProfileSettings = preferences.imageProfileSettings.first()
            videoSettings = preferences.videoSettings.first()
            audioSettings = preferences.audioSettings.first()

            // Build file info list
            val files = uris.mapNotNull { uri ->
                val mimeType = FileUtil.getMimeType(context, uri) ?: return@mapNotNull null
                val name = FileUtil.getFileName(context, uri) ?: "Unknown"
                val size = FileUtil.getFileSize(context, uri)
                val mediaType = FileUtil.getMediaType(mimeType)

                SharedFileInfo(
                    uri = uri,
                    name = name,
                    size = size,
                    mimeType = mimeType,
                    mediaType = mediaType
                )
            }

            if (files.isEmpty()) {
                _uiState.value = CompressUiState.Error("No valid files to compress")
                return@launch
            }

            currentFiles = files

            if (autoCompress) {
                compress()
            } else {
                updatePreview()
            }
        }
    }

    fun updateImageProfile(profile: ImageProfileSettings) {
        imageProfileSettings = profile
        updatePreview()
    }

    fun updateVideoSettings(settings: VideoCompressionSettings) {
        videoSettings = settings
        updatePreview()
    }

    fun updateAudioSettings(settings: AudioCompressionSettings) {
        audioSettings = settings
        updatePreview()
    }

    fun compress() {
        viewModelScope.launch {
            val files = currentFiles
            if (files.isEmpty()) return@launch

            _uiState.value = CompressUiState.Compressing(0f, 0, files.size)

            val results = mutableListOf<CompressionResult>()

            try {
                files.forEachIndexed { index, fileInfo ->
                    _uiState.value = CompressUiState.Compressing(
                        progress = index.toFloat() / files.size,
                        currentFile = index + 1,
                        totalFiles = files.size
                    )

                    // Select per-format image settings based on input mime type
                    val imageSettings = imageProfileSettings.forInputFormat(fileInfo.mimeType)

                    val result = mediaCompressor.compress(
                        uri = fileInfo.uri,
                        mediaType = fileInfo.mediaType,
                        imageSettings = imageSettings,
                        videoSettings = videoSettings,
                        audioSettings = audioSettings
                    )
                    results.add(result)
                }

                _uiState.value = CompressUiState.Done(results)
            } catch (e: Exception) {
                Timber.e(e, "Compression failed")
                _uiState.value = CompressUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun retry() {
        if (currentFiles.isNotEmpty()) {
            updatePreview()
        }
    }

    private fun updatePreview() {
        val estimatedTotal = currentFiles.sumOf { fileInfo ->
            val imageSettings = imageProfileSettings.forInputFormat(fileInfo.mimeType)
            mediaCompressor.estimateCompressedSize(
                uri = fileInfo.uri,
                originalSize = fileInfo.size,
                mediaType = fileInfo.mediaType,
                imageSettings = imageSettings,
                videoSettings = videoSettings,
                audioSettings = audioSettings
            )
        }

        _uiState.value = CompressUiState.Preview(
            files = currentFiles,
            estimatedTotalSize = estimatedTotal,
            imageProfileSettings = imageProfileSettings,
            videoSettings = videoSettings,
            audioSettings = audioSettings
        )
    }
}
