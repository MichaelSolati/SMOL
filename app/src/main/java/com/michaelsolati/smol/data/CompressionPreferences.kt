package com.michaelsolati.smol.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.michaelsolati.smol.data.model.AudioCompressionSettings
import com.michaelsolati.smol.data.model.AudioFormat
import com.michaelsolati.smol.data.model.ImageCompressionSettings
import com.michaelsolati.smol.data.model.ImageFormat
import com.michaelsolati.smol.data.model.ImageProfileSettings
import com.michaelsolati.smol.data.model.VideoCodec
import com.michaelsolati.smol.data.model.VideoCompressionSettings
import com.michaelsolati.smol.data.model.VideoOutputFormat
import com.michaelsolati.smol.data.model.VideoResolution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompressionPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val imageProfileSettings: Flow<ImageProfileSettings> = dataStore.data.map { prefs ->
        ImageProfileSettings(
            jpeg = readImageSettings(prefs, "jpeg", defaultQuality = 80),
            png = readImageSettings(prefs, "png", defaultQuality = 95),
            webp = readImageSettings(prefs, "webp", defaultQuality = 80),
            gif = readImageSettings(prefs, "gif", defaultQuality = 80)
        )
    }

    // Legacy accessor for backward compatibility with CompressViewModel
    val imageSettings: Flow<ImageCompressionSettings> = imageProfileSettings.map { it.jpeg }

    val videoSettings: Flow<VideoCompressionSettings> = dataStore.data.map { prefs ->
        VideoCompressionSettings(
            resolution = prefs[KEY_VIDEO_RESOLUTION]?.takeIf { it.isNotEmpty() }?.let { VideoResolution.valueOf(it) },
            bitrateMbps = prefs[KEY_VIDEO_BITRATE] ?: 0f,
            codec = prefs[KEY_VIDEO_CODEC]?.takeIf { it.isNotEmpty() }?.let { VideoCodec.valueOf(it) },
            outputFormat = prefs[KEY_VIDEO_OUTPUT_FORMAT]?.takeIf { it.isNotEmpty() }?.let { VideoOutputFormat.valueOf(it) },
            maxFileSizeBytes = prefs[KEY_VIDEO_MAX_FILE_SIZE] ?: 0L,
            stripMetadata = prefs[KEY_VIDEO_STRIP_METADATA] ?: false
        )
    }

    val audioSettings: Flow<AudioCompressionSettings> = dataStore.data.map { prefs ->
        AudioCompressionSettings(
            bitrate = prefs[KEY_AUDIO_BITRATE] ?: 0,
            format = prefs[KEY_AUDIO_FORMAT]?.takeIf { it.isNotEmpty() }?.let { AudioFormat.valueOf(it) },
            maxFileSizeBytes = prefs[KEY_AUDIO_MAX_FILE_SIZE] ?: 0L,
            stripMetadata = prefs[KEY_AUDIO_STRIP_METADATA] ?: false
        )
    }

    suspend fun updateImageProfileSettings(profile: ImageProfileSettings) {
        dataStore.edit { prefs ->
            writeImageSettings(prefs, "jpeg", profile.jpeg)
            writeImageSettings(prefs, "png", profile.png)
            writeImageSettings(prefs, "webp", profile.webp)
            writeImageSettings(prefs, "gif", profile.gif)
        }
    }

    suspend fun updateImageSettingsForFormat(formatKey: String, settings: ImageCompressionSettings) {
        dataStore.edit { prefs ->
            writeImageSettings(prefs, formatKey, settings)
        }
    }

    suspend fun updateVideoSettings(settings: VideoCompressionSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_VIDEO_RESOLUTION] = settings.resolution?.name ?: ""
            prefs[KEY_VIDEO_BITRATE] = settings.bitrateMbps
            prefs[KEY_VIDEO_CODEC] = settings.codec?.name ?: ""
            prefs[KEY_VIDEO_OUTPUT_FORMAT] = settings.outputFormat?.name ?: ""
            prefs[KEY_VIDEO_MAX_FILE_SIZE] = settings.maxFileSizeBytes
            prefs[KEY_VIDEO_STRIP_METADATA] = settings.stripMetadata
        }
    }

    suspend fun updateAudioSettings(settings: AudioCompressionSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_AUDIO_BITRATE] = settings.bitrate
            prefs[KEY_AUDIO_FORMAT] = settings.format?.name ?: ""
            prefs[KEY_AUDIO_MAX_FILE_SIZE] = settings.maxFileSizeBytes
            prefs[KEY_AUDIO_STRIP_METADATA] = settings.stripMetadata
        }
    }

    private fun readImageSettings(
        prefs: Preferences,
        prefix: String,
        defaultQuality: Int
    ): ImageCompressionSettings {
        return ImageCompressionSettings(
            quality = prefs[intPreferencesKey("image_${prefix}_quality")] ?: defaultQuality,
            format = prefs[stringPreferencesKey("image_${prefix}_format")]?.takeIf { it.isNotEmpty() }?.let { ImageFormat.valueOf(it) },
            maxResolution = prefs[intPreferencesKey("image_${prefix}_max_resolution")] ?: 0,
            maxFileSizeBytes = prefs[longPreferencesKey("image_${prefix}_max_file_size")] ?: 0L,
            stripMetadata = prefs[booleanPreferencesKey("image_${prefix}_strip_metadata")] ?: false
        )
    }

    private fun writeImageSettings(prefs: MutablePreferences, prefix: String, settings: ImageCompressionSettings) {
        prefs[intPreferencesKey("image_${prefix}_quality")] = settings.quality
        prefs[stringPreferencesKey("image_${prefix}_format")] = settings.format?.name ?: ""
        prefs[intPreferencesKey("image_${prefix}_max_resolution")] = settings.maxResolution
        prefs[longPreferencesKey("image_${prefix}_max_file_size")] = settings.maxFileSizeBytes
        prefs[booleanPreferencesKey("image_${prefix}_strip_metadata")] = settings.stripMetadata
    }

    companion object {
        private val KEY_VIDEO_RESOLUTION = stringPreferencesKey("video_resolution")
        private val KEY_VIDEO_BITRATE = floatPreferencesKey("video_bitrate")
        private val KEY_VIDEO_CODEC = stringPreferencesKey("video_codec")
        private val KEY_VIDEO_OUTPUT_FORMAT = stringPreferencesKey("video_output_format")
        private val KEY_VIDEO_MAX_FILE_SIZE = longPreferencesKey("video_max_file_size")
        private val KEY_VIDEO_STRIP_METADATA = booleanPreferencesKey("video_strip_metadata")
        private val KEY_AUDIO_BITRATE = intPreferencesKey("audio_bitrate")
        private val KEY_AUDIO_FORMAT = stringPreferencesKey("audio_format")
        private val KEY_AUDIO_MAX_FILE_SIZE = longPreferencesKey("audio_max_file_size")
        private val KEY_AUDIO_STRIP_METADATA = booleanPreferencesKey("audio_strip_metadata")
    }
}
