package com.michaelsolati.smol.service

import android.net.Uri
import com.michaelsolati.smol.data.model.AudioCompressionSettings
import com.michaelsolati.smol.data.model.CompressionResult
import com.michaelsolati.smol.data.model.ImageCompressionSettings
import com.michaelsolati.smol.data.model.MediaType
import com.michaelsolati.smol.data.model.VideoCompressionSettings
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaCompressor @Inject constructor(
    private val imageCompressor: ImageCompressor,
    private val videoCompressor: VideoCompressor,
    private val audioCompressor: AudioCompressor
) {

    val videoProgress: StateFlow<Float> get() = videoCompressor.progress
    val audioProgress: StateFlow<Float> get() = audioCompressor.progress

    suspend fun compress(
        uri: Uri,
        mediaType: MediaType,
        imageSettings: ImageCompressionSettings? = null,
        videoSettings: VideoCompressionSettings? = null,
        audioSettings: AudioCompressionSettings? = null
    ): CompressionResult {
        return when (mediaType) {
            MediaType.IMAGE -> imageCompressor.compress(
                uri,
                imageSettings ?: ImageCompressionSettings()
            )
            MediaType.VIDEO -> videoCompressor.compress(
                uri,
                videoSettings ?: VideoCompressionSettings()
            )
            MediaType.AUDIO -> audioCompressor.compress(
                uri,
                audioSettings ?: AudioCompressionSettings()
            )
        }
    }

    fun estimateCompressedSize(
        uri: Uri,
        originalSize: Long,
        mediaType: MediaType,
        imageSettings: ImageCompressionSettings? = null,
        videoSettings: VideoCompressionSettings? = null,
        audioSettings: AudioCompressionSettings? = null
    ): Long {
        return when (mediaType) {
            MediaType.IMAGE -> imageCompressor.estimateCompressedSize(
                originalSize,
                imageSettings ?: ImageCompressionSettings()
            )
            MediaType.VIDEO -> videoCompressor.estimateCompressedSize(
                uri,
                videoSettings ?: VideoCompressionSettings()
            )
            MediaType.AUDIO -> audioCompressor.estimateCompressedSize(
                uri,
                audioSettings ?: AudioCompressionSettings()
            )
        }
    }
}
