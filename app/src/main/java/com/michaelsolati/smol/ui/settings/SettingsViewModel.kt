package com.michaelsolati.smol.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaelsolati.smol.data.CompressionPreferences
import com.michaelsolati.smol.data.model.AudioCompressionSettings
import com.michaelsolati.smol.data.model.AudioFormat
import com.michaelsolati.smol.data.model.ImageCompressionSettings
import com.michaelsolati.smol.data.model.ImageFormat
import com.michaelsolati.smol.data.model.ImageProfileSettings
import com.michaelsolati.smol.data.model.VideoCodec
import com.michaelsolati.smol.data.model.VideoCompressionSettings
import com.michaelsolati.smol.data.model.VideoOutputFormat
import com.michaelsolati.smol.data.model.VideoResolution
import com.michaelsolati.smol.util.ShareUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class NewVersionAvailable(val version: String, val downloadUrl: String) : UpdateState()
    data object UpToDate : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: CompressionPreferences
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    val imageProfileSettings: StateFlow<ImageProfileSettings> = preferences.imageProfileSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ImageProfileSettings())

    val videoSettings: StateFlow<VideoCompressionSettings> = preferences.videoSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VideoCompressionSettings())

    val audioSettings: StateFlow<AudioCompressionSettings> = preferences.audioSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioCompressionSettings())

    fun exportSettingsJson(): String {
        return com.michaelsolati.smol.util.BackupUtil.exportSettings(
            imageProfileSettings.value,
            videoSettings.value,
            audioSettings.value
        )
    }

    fun importSettingsFromJson(jsonString: String): Boolean {
        val backup = com.michaelsolati.smol.util.BackupUtil.importSettings(jsonString) ?: return false
        viewModelScope.launch {
            preferences.updateImageProfileSettings(backup.first)
            preferences.updateVideoSettings(backup.second)
            preferences.updateAudioSettings(backup.third)
        }
        return true
    }

    // Image profile update helpers — each operates on a specific format key
    fun updateImageQuality(formatKey: String, quality: Int) {
        updateImageForFormat(formatKey) { it.copy(quality = quality) }
    }

    fun updateImageFormat(formatKey: String, format: ImageFormat?) {
        updateImageForFormat(formatKey) { it.copy(format = format) }
    }

    fun updateImageMaxResolution(formatKey: String, maxResolution: Int) {
        updateImageForFormat(formatKey) { it.copy(maxResolution = maxResolution) }
    }

    fun updateImageMaxFileSize(formatKey: String, bytes: Long) {
        updateImageForFormat(formatKey) { it.copy(maxFileSizeBytes = bytes) }
    }

    fun updateImageStripMetadata(formatKey: String, strip: Boolean) {
        updateImageForFormat(formatKey) { it.copy(stripMetadata = strip) }
    }

    fun updateVideoResolution(resolution: VideoResolution?) {
        viewModelScope.launch {
            preferences.updateVideoSettings(videoSettings.value.copy(resolution = resolution))
        }
    }

    fun updateVideoBitrate(bitrateMbps: Float) {
        viewModelScope.launch {
            preferences.updateVideoSettings(videoSettings.value.copy(bitrateMbps = bitrateMbps))
        }
    }

    fun updateVideoCodec(codec: VideoCodec?) {
        viewModelScope.launch {
            preferences.updateVideoSettings(videoSettings.value.copy(codec = codec))
        }
    }

    fun updateVideoOutputFormat(format: VideoOutputFormat?) {
        viewModelScope.launch {
            preferences.updateVideoSettings(videoSettings.value.copy(outputFormat = format))
        }
    }

    fun updateVideoMaxFileSize(bytes: Long) {
        viewModelScope.launch {
            preferences.updateVideoSettings(videoSettings.value.copy(maxFileSizeBytes = bytes))
        }
    }

    fun updateVideoStripMetadata(strip: Boolean) {
        viewModelScope.launch {
            preferences.updateVideoSettings(videoSettings.value.copy(stripMetadata = strip))
        }
    }

    fun updateAudioBitrate(bitrate: Int) {
        viewModelScope.launch {
            preferences.updateAudioSettings(audioSettings.value.copy(bitrate = bitrate))
        }
    }

    fun updateAudioFormat(format: AudioFormat?) {
        viewModelScope.launch {
            preferences.updateAudioSettings(audioSettings.value.copy(format = format))
        }
    }

    fun updateAudioMaxFileSize(bytes: Long) {
        viewModelScope.launch {
            preferences.updateAudioSettings(audioSettings.value.copy(maxFileSizeBytes = bytes))
        }
    }

    fun updateAudioStripMetadata(strip: Boolean) {
        viewModelScope.launch {
            preferences.updateAudioSettings(audioSettings.value.copy(stripMetadata = strip))
        }
    }

    private fun updateImageForFormat(formatKey: String, transform: (ImageCompressionSettings) -> ImageCompressionSettings) {
        viewModelScope.launch {
            val current = imageProfileSettings.value
            val currentSettings = when (formatKey) {
                "jpeg" -> current.jpeg
                "png" -> current.png
                "webp" -> current.webp
                "gif" -> current.gif
                else -> return@launch
            }
            preferences.updateImageSettingsForFormat(formatKey, transform(currentSettings))
        }
    }

    fun checkForUpdates(context: Context) {
        _updateState.value = UpdateState.Checking
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentVersion = getAppVersionName(context)
                
                val connection = URL("https://api.github.com/repos/MichaelSolati/SMOL/releases/latest").openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "SMOL-App")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val tagName = json.getString("tag_name")
                
                val isNew = isNewerVersion(currentVersion, tagName)
                if (isNew) {
                    val assets = json.getJSONArray("assets")
                    var downloadUrl = ""
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.getString("name").endsWith(".apk")) {
                            downloadUrl = asset.getString("browser_download_url")
                            break
                        }
                    }
                    if (downloadUrl.isNotEmpty()) {
                        _updateState.value = UpdateState.NewVersionAvailable(tagName, downloadUrl)
                    } else {
                        _updateState.value = UpdateState.Error("No APK found in release assets")
                    }
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.localizedMessage ?: "Failed to check for updates")
            }
        }
    }

    fun downloadAndInstallUpdate(downloadUrl: String, context: Context) {
        _updateState.value = UpdateState.Downloading(0f)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()
                
                val fileLength = connection.contentLength
                val cacheDir = File(context.cacheDir, "compressed")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val apkFile = File(cacheDir, "smol_update.apk")
                if (apkFile.exists()) apkFile.delete()
                
                connection.inputStream.use { input ->
                    apkFile.outputStream().use { output ->
                        val data = ByteArray(4096)
                        var total = 0L
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            if (fileLength > 0) {
                                _updateState.value = UpdateState.Downloading(total.toFloat() / fileLength)
                            }
                            output.write(data, 0, count)
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    _updateState.value = UpdateState.Idle
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _updateState.value = UpdateState.Error(e.localizedMessage ?: "Download failed")
                }
            }
        }
    }

    private fun installApk(context: Context, file: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Please allow installation from unknown sources, then try again.", Toast.LENGTH_LONG).show()
                    return
                }
            }
            
            val apkUri = ShareUtil.getFileProviderUri(context, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Installation failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun getAppVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.1"
        } catch (e: Exception) {
            "0.0.1"
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val currVal = currentParts.getOrNull(i) ?: 0
            val latVal = latestParts.getOrNull(i) ?: 0
            if (latVal > currVal) return true
            if (currVal > latVal) return false
        }
        return false
    }
}
