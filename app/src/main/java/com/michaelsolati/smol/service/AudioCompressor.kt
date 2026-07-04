package com.michaelsolati.smol.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.michaelsolati.smol.data.model.AudioCompressionSettings
import com.michaelsolati.smol.data.model.AudioFormat
import com.michaelsolati.smol.data.model.CompressionResult
import com.michaelsolati.smol.data.model.MediaType
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

interface AudioCompressor {
    val progress: StateFlow<Float>
    suspend fun compress(uri: Uri, settings: AudioCompressionSettings): CompressionResult
    fun estimateCompressedSize(uri: Uri, settings: AudioCompressionSettings): Long
}

@Singleton
class AudioCompressorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioCompressor {

    private val _progress = MutableStateFlow(0f)
    override val progress: StateFlow<Float> = _progress

    override suspend fun compress(uri: Uri, settings: AudioCompressionSettings): CompressionResult {
        val originalSize = withContext(Dispatchers.IO) { FileUtil.getFileSize(context, uri) }
        _progress.value = 0f

        val outputExtension = settings.format?.extension ?: "m4a"
        val outputFile = withContext(Dispatchers.IO) { FileUtil.createCompressedFile(context, outputExtension) }
        val outputPath = outputFile.absolutePath

        val mediaItem = MediaItem.fromUri(uri)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setRemoveVideo(true)
            .build()

        val audioMimeType = when (settings.format) {
            AudioFormat.AAC -> MimeTypes.AUDIO_AAC
            AudioFormat.OGG -> MimeTypes.AUDIO_VORBIS
            AudioFormat.OPUS -> MimeTypes.AUDIO_OPUS
            AudioFormat.FLAC -> MimeTypes.AUDIO_FLAC
            null -> null // keep original codec
        }

        // Calculate effective bitrate if max file size is set
        val effectiveBitrate = if (settings.maxFileSizeBytes > 0) {
            val durationMs = withContext(Dispatchers.IO) { getDuration(uri) }
            if (durationMs > 0) {
                val durationSec = durationMs / 1000.0
                ((settings.maxFileSizeBytes * 8) / durationSec).toLong()
                    .coerceIn(32_000, 320_000) // 32-320 kbps range
            } else {
                settings.bitrate * 1000L
            }
        } else {
            settings.bitrate * 1000L
        }

        // Transformer must be built and started on the main thread
        return withContext(Dispatchers.Main) {
            val transformerBuilder = Transformer.Builder(context)

            // Only set audio MIME type if format is specified
            if (audioMimeType != null) {
                transformerBuilder.setAudioMimeType(audioMimeType)
            }

            val transformer = transformerBuilder.build()

            suspendCancellableCoroutine { continuation ->
                val listener = object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        _progress.value = 1f
                        val compressedUri = ShareUtil.getFileProviderUri(context, outputFile)
                        Timber.d("Audio compressed: %s -> %s (%s -> %s)",
                            uri, compressedUri,
                            FileUtil.formatFileSize(originalSize),
                            FileUtil.formatFileSize(outputFile.length()))

                        continuation.resume(
                            CompressionResult(
                                originalUri = uri,
                                compressedUri = compressedUri,
                                originalSize = originalSize,
                                compressedSize = outputFile.length(),
                                mediaType = MediaType.AUDIO
                            )
                        )
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        Timber.e(exportException, "Audio compression failed")
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

    override fun estimateCompressedSize(uri: Uri, settings: AudioCompressionSettings): Long {
        if (settings.maxFileSizeBytes > 0) {
            return settings.maxFileSizeBytes
        }
        val durationMs = getDuration(uri)
        if (durationMs <= 0) return 0L
        val durationSec = durationMs / 1000.0
        
        val originalBitrate = getOriginalBitrate(uri)
        val bitsPerSecond = if (settings.bitrate > 0) {
            settings.bitrate * 1000L
        } else if (originalBitrate > 0) {
            originalBitrate
        } else {
            128_000L // Fallback: 128 kbps
        }
        return ((bitsPerSecond * durationSec) / 8).toLong()
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
            Timber.w(e, "Failed to get audio duration")
            0L
        }
    }
}
