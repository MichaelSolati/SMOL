package com.michaelsolati.smol.data.model

data class ImageCompressionSettings(
    val quality: Int = 80,
    val format: ImageFormat? = null, // null = keep original format
    val maxResolution: Int = 0, // 0 = no resize (keep original)
    val maxFileSizeBytes: Long = 0L, // 0 = disabled, otherwise target size in bytes
    val stripMetadata: Boolean = false
)

enum class ImageFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp"),
    GIF("gif", "image/gif")
}

/**
 * Holds per-input-format settings for images.
 * Each input format (JPEG, PNG, WEBP) can have its own compression profile.
 */
data class ImageProfileSettings(
    val jpeg: ImageCompressionSettings = ImageCompressionSettings(
        quality = 80,
        format = null,
        maxResolution = 0
    ),
    val png: ImageCompressionSettings = ImageCompressionSettings(
        quality = 95,
        format = null,
        maxResolution = 0
    ),
    val webp: ImageCompressionSettings = ImageCompressionSettings(
        quality = 80,
        format = null,
        maxResolution = 0
    ),
    val gif: ImageCompressionSettings = ImageCompressionSettings(
        quality = 80,
        format = null,
        maxResolution = 0
    )
) {
    fun forInputFormat(inputMimeType: String): ImageCompressionSettings {
        return when {
            inputMimeType.contains("png") -> png
            inputMimeType.contains("webp") -> webp
            inputMimeType.contains("gif") -> gif
            else -> jpeg // JPEG and any other image format
        }
    }
}

data class VideoCompressionSettings(
    val resolution: VideoResolution? = null, // null = keep original
    val bitrateMbps: Float = 0f, // 0 = keep original bitrate
    val codec: VideoCodec? = null, // null = keep original codec
    val outputFormat: VideoOutputFormat? = null, // null = keep original container
    val maxFileSizeBytes: Long = 0L,
    val stripMetadata: Boolean = false
)

/**
 * Video resolution defined by short edge height.
 * This is orientation-agnostic: "720p" means the shorter dimension is 720px,
 * so a landscape video becomes 1280x720 and a portrait video becomes 720x1280.
 */
enum class VideoResolution(val shortEdge: Int, val label: String) {
    SD_360(360, "360p"),
    SD_480(480, "480p"),
    HD_720(720, "720p"),
    HD_1080(1080, "1080p"),
    QHD_1440(1440, "1440p"),
    UHD_4K(2160, "4K")
}

enum class VideoCodec(val mimeType: String, val label: String) {
    H264("video/avc", "H.264"),
    H265("video/hevc", "H.265"),
    VP8("video/x-vnd.on2.vp8", "VP8"),
    VP9("video/x-vnd.on2.vp9", "VP9")
}

enum class VideoOutputFormat(val extension: String, val label: String) {
    MP4("mp4", "MP4"),
    WEBM("webm", "WebM")
}

data class AudioCompressionSettings(
    val bitrate: Int = 0, // 0 = keep original bitrate
    val format: AudioFormat? = null, // null = keep original format
    val maxFileSizeBytes: Long = 0L,
    val stripMetadata: Boolean = false
)

enum class AudioFormat(val extension: String, val mimeType: String, val label: String) {
    AAC("m4a", "audio/mp4a-latm", "AAC"),
    OGG("ogg", "audio/ogg", "OGG"),
    OPUS("opus", "audio/opus", "Opus"),
    FLAC("flac", "audio/flac", "FLAC")
}

