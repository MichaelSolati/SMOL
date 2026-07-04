package com.michaelsolati.smol.service

import android.net.Uri
import com.michaelsolati.smol.data.model.AudioCompressionSettings
import com.michaelsolati.smol.data.model.CompressionResult
import com.michaelsolati.smol.data.model.ImageCompressionSettings
import com.michaelsolati.smol.data.model.MediaType
import com.michaelsolati.smol.data.model.VideoCompressionSettings
import android.content.Context
import com.michaelsolati.smol.util.FileUtil
import com.michaelsolati.smol.util.ShareUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
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
        val result = when (mediaType) {
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

        if (result.compressedSize >= result.originalSize && result.originalSize > 0) {
            try {
                // If compression grew the file or kept it identical, fallback to copying the original file
                val originalName = FileUtil.getFileName(context, uri) ?: "temp"
                val extension = originalName.substringAfterLast('.', "tmp")
                val fallbackUri = ShareUtil.copyToCache(context, uri, extension)
                return result.copy(
                    compressedUri = fallbackUri,
                    compressedSize = result.originalSize
                )
            } catch (e: Exception) {
                return result
            }
        }
        return result
    }

    fun estimateCompressedSize(
        uri: Uri,
        originalSize: Long,
        mediaType: MediaType,
        imageSettings: ImageCompressionSettings? = null,
        videoSettings: VideoCompressionSettings? = null,
        audioSettings: AudioCompressionSettings? = null
    ): Long {
        val estimated = when (mediaType) {
            MediaType.IMAGE -> imageCompressor.estimateCompressedSize(
                uri,
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
        return if (estimated >= originalSize && originalSize > 0) originalSize else estimated
    }
}
