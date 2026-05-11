package com.michaelsolati.smol.data.model

import android.net.Uri

data class CompressionResult(
    val originalUri: Uri,
    val compressedUri: Uri,
    val originalSize: Long,
    val compressedSize: Long,
    val mediaType: MediaType
)
