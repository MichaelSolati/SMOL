package com.michaelsolati.smol.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File

object ShareUtil {

    fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun createShareIntent(context: Context, uris: List<Uri>, mimeType: String): Intent {
        return if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    fun copyToCache(context: Context, uri: Uri, extension: String): Uri {
        val file = FileUtil.createCompressedFile(context, extension)
        FileUtil.copyUriToFile(context, uri, file)
        return getFileProviderUri(context, file)
    }

    /**
     * Saves a compressed file to the device's Downloads folder via MediaStore.
     * Returns the content URI of the saved file, or null on failure.
     */
    fun saveToDownloads(context: Context, uri: Uri, fileName: String, mimeType: String): Uri? {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SMOL")
            }

            val resolver = context.contentResolver
            val destUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return null

            resolver.openOutputStream(destUri)?.use { output ->
                resolver.openInputStream(uri)?.use { input ->
                    input.copyTo(output)
                }
            }

            Timber.d("Saved to downloads: %s", destUri)
            destUri
        } catch (e: Exception) {
            Timber.e(e, "Failed to save to downloads")
            null
        }
    }

    fun extractSharedUris(intent: Intent): List<Uri> {
        val uris = mutableListOf<Uri>()
        when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let {
                    uris.add(it)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let {
                    uris.addAll(it)
                }
            }
        }
        return uris
    }
}
