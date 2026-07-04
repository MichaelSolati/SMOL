package com.michaelsolati.smol.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.michaelsolati.smol.data.model.CompressionResult
import com.michaelsolati.smol.data.model.MediaType
import com.michaelsolati.smol.data.model.VideoCompressionSettings
import com.michaelsolati.smol.data.model.VideoOutputFormat
import com.michaelsolati.smol.util.FileUtil
import com.michaelsolati.smol.util.ShareUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface VideoCompressor {
    val progress: StateFlow<Float>
    suspend fun compress(uri: Uri, settings: VideoCompressionSettings): CompressionResult
    fun estimateCompressedSize(uri: Uri, settings: VideoCompressionSettings): Long
}

@Singleton
class VideoCompressorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VideoCompressor {

    private val _progress = MutableStateFlow(0f)
    override val progress: StateFlow<Float> = _progress

    override suspend fun compress(uri: Uri, settings: VideoCompressionSettings): CompressionResult {
        val originalSize = withContext(Dispatchers.IO) { FileUtil.getFileSize(context, uri) }
        _progress.value = 0f

        val outputExtension = settings.outputFormat?.extension ?: "mp4"
        val outputFile = withContext(Dispatchers.IO) { FileUtil.createCompressedFile(context, outputExtension) }
        val outputPath = outputFile.absolutePath

        // Determine the effective bitrate
        val effectiveBitrate = if (settings.maxFileSizeBytes > 0) {
            val durationMs = withContext(Dispatchers.IO) { getDuration(uri) }
            if (durationMs > 0) {
                val durationSec = durationMs / 1000.0
                ((settings.maxFileSizeBytes * 8) / durationSec).toLong()
                    .coerceAtLeast(100_000) // minimum 100 kbps
            } else {
                (settings.bitrateMbps * 1_000_000).toLong()
            }
        } else {
            (settings.bitrateMbps * 1_000_000).toLong()
        }

        // Build video effects for resolution scaling (only if resolution is specified)
        val videoEffects: List<Effect> = if (settings.resolution != null) {
            listOf(Presentation.createForHeight(settings.resolution.shortEdge))
        } else {
            emptyList()
        }

        val mediaItem = MediaItem.fromUri(uri)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()

        val audioMimeType = when (settings.outputFormat) {
            VideoOutputFormat.WEBM -> MimeTypes.AUDIO_VORBIS
            VideoOutputFormat.MP4 -> MimeTypes.AUDIO_AAC
            null -> MimeTypes.AUDIO_AAC // default
        }

        // Transformer must be built and started on the main thread
        return withContext(Dispatchers.Main) {
            val transformerBuilder = Transformer.Builder(context)
                .setAudioMimeType(audioMimeType)

            // Only set video MIME type if codec is specified
            if (settings.codec != null) {
                transformerBuilder.setVideoMimeType(settings.codec.mimeType)
            }

            val transformer = transformerBuilder.build()

            suspendCancellableCoroutine { continuation ->
                val listener = object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        _progress.value = 1f
                        val compressedUri = ShareUtil.getFileProviderUri(context, outputFile)
                        Timber.d("Video compressed: %s -> %s (%s -> %s)",
                            uri, compressedUri,
                            FileUtil.formatFileSize(originalSize),
                            FileUtil.formatFileSize(outputFile.length()))

                        continuation.resume(
                            CompressionResult(
                                originalUri = uri,
                                compressedUri = compressedUri,
                                originalSize = originalSize,
                                compressedSize = outputFile.length(),
                                mediaType = MediaType.VIDEO
                            )
                        )
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        Timber.e(exportException, "Video compression failed")
                        outputFile.delete()
                        continuation.resumeWithException(exportException)
                    }
                }

                transformer.addListener(listener)
                transformer.start(editedMediaItem, outputPath)

                continuation.invokeOnCancellation {
                    transformer.cancel()
                    outputFile.delete()
                }
            }
        }
    }

    override fun estimateCompressedSize(uri: Uri, settings: VideoCompressionSettings): Long {
        if (settings.maxFileSizeBytes > 0) {
            return settings.maxFileSizeBytes
        }
        val durationMs = getDuration(uri)
        if (durationMs <= 0) return 0L
        val durationSec = durationMs / 1000.0
        
        val originalBitrate = getOriginalBitrate(uri)
        val bitsPerSecond = if (settings.bitrateMbps > 0f) {
            (settings.bitrateMbps * 1_000_000).toLong()
        } else if (originalBitrate > 0) {
            originalBitrate
        } else {
            4_000_000L // Fallback: 4 Mbps
        }
        
        var estimated = ((bitsPerSecond * durationSec) / 8).toLong()
        
        // Adjust for resolution downscaling if resolution is specified
        if (settings.resolution != null && originalBitrate > 0) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val originalHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toDoubleOrNull() ?: 0.0
                retriever.release()
                if (originalHeight > settings.resolution.shortEdge && originalHeight > 0) {
                    val scale = settings.resolution.shortEdge.toDouble() / originalHeight
                    estimated = (estimated * Math.pow(scale, 1.2)).toLong()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        return estimated
    }

    private fun getOriginalBitrate(uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            retriever.release()
            bitrate
        } catch (e: Exception) {
            0L
        }
    }

    private fun getDuration(uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            duration
        } catch (e: Exception) {
            Timber.w(e, "Failed to get video duration")
            0L
        }
    }
}
