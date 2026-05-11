package com.michaelsolati.smol.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.michaelsolati.smol.data.model.MediaType
import java.io.File
import java.io.InputStream
import java.text.DecimalFormat

object FileUtil {

    fun getFileSize(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex != -1) {
                return cursor.getLong(sizeIndex)
            }
        }
        return 0L
    }

    fun getFileName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    fun getMediaType(mimeType: String): MediaType {
        return when {
            mimeType.startsWith("image/") -> MediaType.IMAGE
            mimeType.startsWith("video/") -> MediaType.VIDEO
            mimeType.startsWith("audio/") -> MediaType.AUDIO
            else -> MediaType.IMAGE
        }
    }

    fun createCompressedFile(context: Context, extension: String): File {
        val dir = File(context.cacheDir, "compressed")
        if (!dir.exists()) dir.mkdirs()
        return File.createTempFile("smol_", ".$extension", dir)
    }

    fun copyUriToFile(context: Context, uri: Uri, file: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun openInputStream(context: Context, uri: Uri): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val format = DecimalFormat("#,##0.#")
        return "${format.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    fun cleanCompressedCache(context: Context, maxAgeMs: Long = 3600000L) {
        val dir = File(context.cacheDir, "compressed")
        if (!dir.exists()) return
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > maxAgeMs) {
                file.delete()
            }
        }
    }
}
