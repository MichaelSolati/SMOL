package com.michaelsolati.smol.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelsolati.smol.data.CompressionPreferences
import com.michaelsolati.smol.data.model.AudioCompressionSettings
import com.michaelsolati.smol.data.model.AudioFormat
import com.michaelsolati.smol.data.model.ImageCompressionSettings
import com.michaelsolati.smol.data.model.ImageFormat
import com.michaelsolati.smol.data.model.ImageProfileSettings
import com.michaelsolati.smol.data.model.VideoCodec
import com.michaelsolati.smol.data.model.VideoCompressionSettings
import com.michaelsolati.smol.data.model.VideoOutputFormat
import com.michaelsolati.smol.data.model.VideoResolution
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: CompressionPreferences
) : ViewModel() {

    val imageProfileSettings: StateFlow<ImageProfileSettings> = preferences.imageProfileSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ImageProfileSettings())

    val videoSettings: StateFlow<VideoCompressionSettings> = preferences.videoSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VideoCompressionSettings())

    val audioSettings: StateFlow<AudioCompressionSettings> = preferences.audioSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioCompressionSettings())

    fun exportSettingsJson(): String {
        return com.michaelsolati.smol.util.BackupUtil.exportSettings(
            imageProfileSettings.value,
            videoSettings.value,
            audioSettings.value
        )
    }

    fun importSettingsFromJson(jsonString: String): Boolean {
        val backup = com.michaelsolati.smol.util.BackupUtil.importSettings(jsonString) ?: return false
        viewModelScope.launch {
            preferences.updateImageProfileSettings(backup.first)
            preferences.updateVideoSettings(backup.second)
            preferences.updateAudioSettings(backup.third)
        }
        return true
    }

    // Image profile update helpers — each operates on a specific format key
    fun updateImageQuality(formatKey: String, quality: Int) {
        updateImageForFormat(formatKey) { it.copy(quality = quality) }
    }

    fun updateImageFormat(formatKey: String, format: ImageFormat?) {
        updateImageForFormat(formatKey) { it.copy(format = format) }
    }

    fun updateImageMaxResolution(formatKey: String, maxResolution: Int) {
        updateImageForFormat(formatKey) { it.copy(maxResolution = maxResolution) }
    }

    fun updateImageMaxFileSize(formatKey: String, bytes: Long) {
        updateImageForFormat(formatKey) { it.copy(maxFileSizeBytes = bytes) }
    }

    fun updateImageStripMetadata(formatKey: String, strip: Boolean) {
        updateImageForFormat(formatKey) { it.copy(stripMetadata = strip) }
    }

    fun updateVideoResolution(resolution: VideoResolution?) {
        viewModelScope.launch {
            preferences.updateVideoSettings(videoSettings.value.copy(resolution = resolution))
        }
    }

    fun updateVideoBitrate(bitrateMbps: Float) {
        viewModelScope.launch {
            preferences.updateVideoSettings(videoSettings.value.copy(bitrateMbps = bitrateMbps))
        }
    }

    fun updateVideoCodec(codec: VideoCodec?) {
        viewModelScope.launch {
            preferences.updateVideoSettings(videoSettings.value.copy(codec = codec))
        }
    }

    fun updateVideoOutputFormat(format: VideoOutputFormat?) {
        viewModelScope.launch {
            preferences.updateVideoSettings(videoSettings.value.copy(outputFormat = format))
        }
    }

    fun updateVideoMaxFileSize(bytes: Long) {
        viewModelScope.launch {
            preferences.updateVideoSettings(videoSettings.value.copy(maxFileSizeBytes = bytes))
        }
    }

    fun updateVideoStripMetadata(strip: Boolean) {
        viewModelScope.launch {
            preferences.updateVideoSettings(videoSettings.value.copy(stripMetadata = strip))
        }
    }

    fun updateAudioBitrate(bitrate: Int) {
        viewModelScope.launch {
            preferences.updateAudioSettings(audioSettings.value.copy(bitrate = bitrate))
        }
    }

    fun updateAudioFormat(format: AudioFormat?) {
        viewModelScope.launch {
            preferences.updateAudioSettings(audioSettings.value.copy(format = format))
        }
    }

    fun updateAudioMaxFileSize(bytes: Long) {
        viewModelScope.launch {
            preferences.updateAudioSettings(audioSettings.value.copy(maxFileSizeBytes = bytes))
        }
    }

    fun updateAudioStripMetadata(strip: Boolean) {
        viewModelScope.launch {
            preferences.updateAudioSettings(audioSettings.value.copy(stripMetadata = strip))
        }
    }

    private fun updateImageForFormat(formatKey: String, transform: (ImageCompressionSettings) -> ImageCompressionSettings) {
        viewModelScope.launch {
            val current = imageProfileSettings.value
            val currentSettings = when (formatKey) {
                "jpeg" -> current.jpeg
                "png" -> current.png
                "webp" -> current.webp
                "gif" -> current.gif
                else -> return@launch
            }
            preferences.updateImageSettingsForFormat(formatKey, transform(currentSettings))
        }
    }
}
