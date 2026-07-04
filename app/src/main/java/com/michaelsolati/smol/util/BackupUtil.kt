package com.michaelsolati.smol.util

import com.michaelsolati.smol.data.model.*
import org.json.JSONObject

object BackupUtil {

    fun exportSettings(
        imageProfile: ImageProfileSettings,
        video: VideoCompressionSettings,
        audio: AudioCompressionSettings
    ): String {
        val root = JSONObject()
        root.put("version", 1)

        // Image Profile
        val imgProfileJson = JSONObject()
        imgProfileJson.put("jpeg", imageSettingsToJson(imageProfile.jpeg))
        imgProfileJson.put("png", imageSettingsToJson(imageProfile.png))
        imgProfileJson.put("webp", imageSettingsToJson(imageProfile.webp))
        imgProfileJson.put("gif", imageSettingsToJson(imageProfile.gif))
        root.put("imageProfile", imgProfileJson)

        // Video
        val videoJson = JSONObject()
        videoJson.put("resolution", video.resolution?.name ?: JSONObject.NULL)
        videoJson.put("bitrateMbps", video.bitrateMbps.toDouble())
        videoJson.put("codec", video.codec?.name ?: JSONObject.NULL)
        videoJson.put("outputFormat", video.outputFormat?.name ?: JSONObject.NULL)
        videoJson.put("maxFileSizeBytes", video.maxFileSizeBytes)
        videoJson.put("stripMetadata", video.stripMetadata)
        root.put("video", videoJson)

        // Audio
        val audioJson = JSONObject()
        audioJson.put("bitrate", audio.bitrate)
        audioJson.put("format", audio.format?.name ?: JSONObject.NULL)
        audioJson.put("maxFileSizeBytes", audio.maxFileSizeBytes)
        audioJson.put("stripMetadata", audio.stripMetadata)
        root.put("audio", audioJson)

        return root.toString(4) // 4 indentation spaces
    }

    fun importSettings(
        jsonString: String
    ): Triple<ImageProfileSettings, VideoCompressionSettings, AudioCompressionSettings>? {
        return try {
            val root = JSONObject(jsonString)
            
            // Image Profile
            val imgProfileJson = root.getJSONObject("imageProfile")
            val imageProfile = ImageProfileSettings(
                jpeg = jsonToImageSettings(imgProfileJson.getJSONObject("jpeg")),
                png = jsonToImageSettings(imgProfileJson.getJSONObject("png")),
                webp = jsonToImageSettings(imgProfileJson.getJSONObject("webp")),
                gif = jsonToImageSettings(imgProfileJson.getJSONObject("gif"))
            )

            // Video
            val videoJson = root.getJSONObject("video")
            val video = VideoCompressionSettings(
                resolution = if (videoJson.isNull("resolution")) null else VideoResolution.valueOf(videoJson.getString("resolution")),
                bitrateMbps = videoJson.getDouble("bitrateMbps").toFloat(),
                codec = if (videoJson.isNull("codec")) null else VideoCodec.valueOf(videoJson.getString("codec")),
                outputFormat = if (videoJson.isNull("outputFormat")) null else VideoOutputFormat.valueOf(videoJson.getString("outputFormat")),
                maxFileSizeBytes = videoJson.getLong("maxFileSizeBytes"),
                stripMetadata = videoJson.getBoolean("stripMetadata")
            )

            // Audio
            val audioJson = root.getJSONObject("audio")
            val audio = AudioCompressionSettings(
                bitrate = audioJson.getInt("bitrate"),
                format = if (audioJson.isNull("format")) null else AudioFormat.valueOf(audioJson.getString("format")),
                maxFileSizeBytes = audioJson.getLong("maxFileSizeBytes"),
                stripMetadata = audioJson.getBoolean("stripMetadata")
            )

            Triple(imageProfile, video, audio)
        } catch (e: Exception) {
            null
        }
    }

    private fun imageSettingsToJson(settings: ImageCompressionSettings): JSONObject {
        val json = JSONObject()
        json.put("quality", settings.quality)
        json.put("format", settings.format?.name ?: JSONObject.NULL)
        json.put("maxResolution", settings.maxResolution)
        json.put("maxFileSizeBytes", settings.maxFileSizeBytes)
        json.put("stripMetadata", settings.stripMetadata)
        return json
    }

    private fun jsonToImageSettings(json: JSONObject): ImageCompressionSettings {
        return ImageCompressionSettings(
            quality = json.getInt("quality"),
            format = if (json.isNull("format")) null else ImageFormat.valueOf(json.getString("format")),
            maxResolution = json.getInt("maxResolution"),
            maxFileSizeBytes = json.getLong("maxFileSizeBytes"),
            stripMetadata = json.getBoolean("stripMetadata")
        )
    }
}
