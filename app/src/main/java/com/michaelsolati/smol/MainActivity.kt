package com.michaelsolati.smol

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.michaelsolati.smol.ui.compress.CompressUiState
import com.michaelsolati.smol.ui.compress.CompressViewModel
import com.michaelsolati.smol.ui.navigation.Screen
import com.michaelsolati.smol.ui.navigation.SmolNavGraph
import com.michaelsolati.smol.ui.theme.SmolTheme
import com.michaelsolati.smol.util.FileUtil
import com.michaelsolati.smol.util.ShareUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val compressViewModel: CompressViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedUris = ShareUtil.extractSharedUris(intent)
        val isQuickCompress = isQuickCompressMode(intent)

        if (isQuickCompress) {
            setTheme(R.style.Theme_SMOL_Transparent)
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (sharedUris.isNotEmpty() && isQuickCompress) {
            // Quick compress: no UI, compress and share immediately
            compressViewModel.loadSharedFiles(sharedUris, autoCompress = true)
            observeQuickCompress()
            // Show minimal transparent activity while compressing
            setContent {}
            return
        }

        val startDestination = if (sharedUris.isNotEmpty()) {
            compressViewModel.loadSharedFiles(sharedUris, autoCompress = false)
            Screen.Compress.route
        } else {
            Screen.Settings.route
        }

        setContent {
            SmolTheme {
                val navController = rememberNavController()
                SmolNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                    compressViewModel = compressViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val sharedUris = ShareUtil.extractSharedUris(intent)
        if (sharedUris.isNotEmpty()) {
            val isQuickCompress = isQuickCompressMode(intent)
            compressViewModel.loadSharedFiles(sharedUris, autoCompress = isQuickCompress)
            if (isQuickCompress) {
                observeQuickCompress()
            }
        }
    }

    private fun observeQuickCompress() {
        lifecycleScope.launch {
            compressViewModel.uiState.collect { state ->
                when (state) {
                    is CompressUiState.Done -> {
                        val totalOriginal = state.results.sumOf { it.originalSize }
                        val totalCompressed = state.results.sumOf { it.compressedSize }
                        val savingsPercent = if (totalOriginal > 0) {
                            ((totalOriginal - totalCompressed) * 100 / totalOriginal).toInt()
                        } else 0

                        val savings = if (savingsPercent > 0) " (saved $savingsPercent%)" else ""
                        val message = "${FileUtil.formatFileSize(totalOriginal)} → ${FileUtil.formatFileSize(totalCompressed)}$savings"
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()

                        // Fire share intent
                        val uris = state.results.map { it.compressedUri }
                        val mimeType = state.results.first().let { result ->
                            when (result.mediaType) {
                                com.michaelsolati.smol.data.model.MediaType.IMAGE -> "image/*"
                                com.michaelsolati.smol.data.model.MediaType.VIDEO -> "video/*"
                                com.michaelsolati.smol.data.model.MediaType.AUDIO -> "audio/*"
                            }
                        }
                        val shareIntent = ShareUtil.createShareIntent(this@MainActivity, uris, mimeType)
                        startActivity(Intent.createChooser(shareIntent, null))
                        finish()
                    }
                    is CompressUiState.Error -> {
                        Toast.makeText(this@MainActivity, "Compression failed: ${state.message}", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    else -> { /* waiting */ }
                }
            }
        }
    }

    private fun isQuickCompressMode(intent: Intent): Boolean {
        val component = intent.component ?: return false
        return try {
            val activityInfo = packageManager.getActivityInfo(component, PackageManager.GET_META_DATA)
            activityInfo.metaData?.getBoolean("com.michaelsolati.smol.QUICK_COMPRESS", false) ?: false
        } catch (e: Exception) {
            false
        }
    }
}
