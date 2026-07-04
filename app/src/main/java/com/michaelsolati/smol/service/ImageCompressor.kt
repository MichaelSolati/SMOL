package com.michaelsolati.smol.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.michaelsolati.smol.data.model.CompressionResult
import com.michaelsolati.smol.data.model.ImageCompressionSettings
import com.michaelsolati.smol.data.model.ImageFormat
import com.michaelsolati.smol.data.model.MediaType
import com.michaelsolati.smol.util.FileUtil
import com.michaelsolati.smol.util.ShareUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface ImageCompressor {
    suspend fun compress(uri: Uri, settings: ImageCompressionSettings): CompressionResult
    fun estimateCompressedSize(uri: Uri, originalSize: Long, settings: ImageCompressionSettings): Long
}

@Singleton
class ImageCompressorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageCompressor {

    override suspend fun compress(uri: Uri, settings: ImageCompressionSettings): CompressionResult = withContext(Dispatchers.IO) {
        val originalSize = FileUtil.getFileSize(context, uri)

        val inputStream = FileUtil.openInputStream(context, uri)
            ?: throw IllegalStateException("Cannot open input stream for $uri")

        // Decode bounds first to calculate sample size
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        inputStream.use { BitmapFactory.decodeStream(it, null, options) }

        val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, settings.maxResolution)

        // Decode with sample size
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val rawBitmap = FileUtil.openInputStream(context, uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalStateException("Cannot decode bitmap from $uri")

        // Apply EXIF rotation (always apply rotation so image displays correctly)
        val bitmap = applyExifRotation(uri, rawBitmap)

        // Scale down if still larger than maxResolution
        val scaledBitmap = scaleToMaxResolution(bitmap, settings.maxResolution)

        // Determine effective format: null means keep original (infer from input MIME type)
        val inputMimeType = FileUtil.getMimeType(context, uri) ?: "image/jpeg"
        val effectiveFormat = settings.format ?: when {
            inputMimeType.contains("png") -> ImageFormat.PNG
            inputMimeType.contains("webp") -> ImageFormat.WEBP
            inputMimeType.contains("gif") -> ImageFormat.GIF
            else -> ImageFormat.JPEG
        }

        val compressFormat = when (effectiveFormat) {
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageFormat.WEBP -> Bitmap.CompressFormat.WEBP_LOSSY
            ImageFormat.GIF -> Bitmap.CompressFormat.WEBP_LOSSY // GIF can't be encoded via Bitmap; fall back to WebP
        }

        val outputExtension = if (effectiveFormat == ImageFormat.GIF) "webp" else effectiveFormat.extension

        // If target file size is set, use iterative compression
        val outputFile = if (settings.maxFileSizeBytes > 0 && effectiveFormat != ImageFormat.PNG) {
            compressToTargetSize(scaledBitmap, compressFormat, settings)
        } else {
            // Standard single-pass compression
            val file = FileUtil.createCompressedFile(context, outputExtension)
            file.outputStream().use { output ->
                scaledBitmap.compress(compressFormat, settings.quality, output)
            }
            file
        }

        // Clean up bitmaps
        if (scaledBitmap !== bitmap) scaledBitmap.recycle()
        if (bitmap !== rawBitmap) bitmap.recycle()
        rawBitmap.recycle()

        // Strip metadata if requested (write a clean file without EXIF)
        if (!settings.stripMetadata && effectiveFormat == ImageFormat.JPEG) {
            // Copy original EXIF to output (minus orientation since we already rotated)
            copyExifData(uri, outputFile.absolutePath)
        }
        // If stripMetadata is true, we just don't write any EXIF — the bitmap compress
        // output is already metadata-free

        val compressedSize = outputFile.length()

        // If compression grew the file, discard the output and pass through the original
        if (compressedSize >= originalSize && originalSize > 0) {
            Timber.d("Image compression grew file (%s -> %s), passing through original",
                FileUtil.formatFileSize(originalSize),
                FileUtil.formatFileSize(compressedSize))
            outputFile.delete()
            val passExtension = effectiveFormat.extension
            val originalFileProviderUri = ShareUtil.copyToCache(context, uri, passExtension)
            return@withContext CompressionResult(
                originalUri = uri,
                compressedUri = originalFileProviderUri,
                originalSize = originalSize,
                compressedSize = originalSize,
                mediaType = MediaType.IMAGE
            )
        }

        val compressedUri = ShareUtil.getFileProviderUri(context, outputFile)
        Timber.d("Image compressed: %s -> %s (%s -> %s)",
            uri, compressedUri,
            FileUtil.formatFileSize(originalSize),
            FileUtil.formatFileSize(compressedSize))

        CompressionResult(
            originalUri = uri,
            compressedUri = compressedUri,
            originalSize = originalSize,
            compressedSize = compressedSize,
            mediaType = MediaType.IMAGE
        )
    }

    override fun estimateCompressedSize(uri: Uri, originalSize: Long, settings: ImageCompressionSettings): Long {
        if (settings.maxFileSizeBytes > 0) {
            return settings.maxFileSizeBytes
        }
        val qualityFactor = settings.quality / 100.0
        val formatFactor = when (settings.format) {
            ImageFormat.JPEG -> 0.7
            ImageFormat.PNG -> 0.9
            ImageFormat.WEBP -> 0.6
            ImageFormat.GIF -> 0.6
            null -> 0.7 // assume JPEG-like ratio when keeping original
        }
        
        var resolutionScale = 1.0
        if (settings.maxResolution > 0) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    val originalWidth = options.outWidth
                    val originalHeight = options.outHeight
                    val maxDim = maxOf(originalWidth, originalHeight)
                    if (maxDim > settings.maxResolution && maxDim > 0) {
                        val scale = settings.maxResolution.toDouble() / maxDim
                        resolutionScale = Math.pow(scale, 1.4)
                    }
                }
            } catch (e: Exception) {
                // Ignore and use 1.0
            }
        }
        
        return (originalSize * qualityFactor * formatFactor * resolutionScale).toLong().coerceAtLeast(1024)
    }

    private fun compressToTargetSize(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        settings: ImageCompressionSettings
    ): java.io.File {
        val targetBytes = settings.maxFileSizeBytes
        var quality = settings.quality
        val effectiveFmt = settings.format ?: ImageFormat.JPEG
        val ext = if (effectiveFmt == ImageFormat.GIF) "webp" else effectiveFmt.extension
        var outputFile = FileUtil.createCompressedFile(context, ext)

        // Binary search for the right quality to hit target size
        var low = 5
        var high = quality
        var bestFile = outputFile

        // First attempt at configured quality
        outputFile.outputStream().use { output ->
            bitmap.compress(format, quality, output)
        }

        if (outputFile.length() <= targetBytes) {
            return outputFile
        }

        // Iteratively reduce quality until we hit the target
        repeat(8) {
            quality = (low + high) / 2
            val tempFile = FileUtil.createCompressedFile(context, ext)
            tempFile.outputStream().use { output ->
                bitmap.compress(format, quality, output)
            }

            if (tempFile.length() <= targetBytes) {
                // Good — try higher quality
                bestFile.delete()
                bestFile = tempFile
                low = quality + 1
            } else {
                // Too big — try lower quality
                tempFile.delete()
                high = quality - 1
            }
        }

        // Final pass: if we never hit target, use lowest quality result
        if (bestFile.length() > targetBytes || !bestFile.exists()) {
            bestFile = FileUtil.createCompressedFile(context, ext)
            bestFile.outputStream().use { output ->
                bitmap.compress(format, low.coerceAtLeast(5), output)
            }
        }

        // Clean up the initial file if it's different from best
        if (outputFile != bestFile && outputFile.exists()) {
            outputFile.delete()
        }

        return bestFile
    }

    private fun copyExifData(sourceUri: Uri, destPath: String) {
        try {
            val sourceStream = context.contentResolver.openInputStream(sourceUri) ?: return
            val sourceExif = sourceStream.use { ExifInterface(it) }
            val destExif = ExifInterface(destPath)

            val tagsToPreserve = listOf(
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_ISO_SPEED,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.TAG_IMAGE_WIDTH,
                ExifInterface.TAG_IMAGE_LENGTH
            )

            for (tag in tagsToPreserve) {
                sourceExif.getAttribute(tag)?.let { value ->
                    destExif.setAttribute(tag, value)
                }
            }

            // Always set orientation to normal since we already rotated
            destExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            destExif.saveAttributes()
        } catch (e: Exception) {
            Timber.w(e, "Failed to copy EXIF data")
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxResolution: Int): Int {
        if (maxResolution <= 0) return 1 // 0 = keep original, no downsampling
        var sampleSize = 1
        val maxDim = maxOf(width, height)
        while (maxDim / sampleSize > maxResolution * 2) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleToMaxResolution(bitmap: Bitmap, maxResolution: Int): Bitmap {
        if (maxResolution <= 0) return bitmap // 0 = keep original
        val maxDim = maxOf(bitmap.width, bitmap.height)
        if (maxDim <= maxResolution) return bitmap

        val scale = maxResolution.toFloat() / maxDim
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = inputStream.use { ExifInterface(it) }
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Timber.w(e, "Failed to read EXIF data")
            bitmap
        }
    }
}
