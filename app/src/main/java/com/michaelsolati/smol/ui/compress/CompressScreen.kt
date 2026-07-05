package com.michaelsolati.smol.ui.compress

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.michaelsolati.smol.data.model.MediaType
import com.michaelsolati.smol.ui.components.SmolTopAppBar
import com.michaelsolati.smol.util.FileUtil
import com.michaelsolati.smol.util.ShareUtil

@Composable
fun CompressScreen(
    viewModel: CompressViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.loadSharedFiles(uris, autoCompress = true)
        } else {
            (context as? Activity)?.finish()
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (viewModel.isPickMode && state is CompressUiState.Idle) {
            filePickerLauncher.launch(arrayOf("image/*", "video/*", "audio/*"))
        } else if (viewModel.isPickMode && state is CompressUiState.Done) {
            val uris = state.results.map { it.compressedUri }
            val resultIntent = Intent().apply {
                if (uris.size == 1) {
                    data = uris.first()
                } else {
                    val clipData = ClipData.newUri(context.contentResolver, "Compressed Files", uris.first())
                    for (i in 1 until uris.size) {
                        clipData.addItem(ClipData.Item(uris[i]))
                    }
                    this.clipData = clipData
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            (context as? Activity)?.let { activity ->
                activity.setResult(Activity.RESULT_OK, resultIntent)
                activity.finish()
            }
        }
    }

    Scaffold(
        topBar = {
            SmolTopAppBar(
                title = "SMOL Compress",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is CompressUiState.Idle, is CompressUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is CompressUiState.Preview -> {
                    PreviewContent(
                        state = state,
                        viewModel = viewModel,
                        onCompress = { viewModel.compress() }
                    )
                }

                is CompressUiState.Compressing -> {
                    CompressingContent(state = state)
                }

                is CompressUiState.Done -> {
                    DoneContent(
                        state = state,
                        isPickMode = viewModel.isPickMode,
                        onShare = { results ->
                            val uris = results.map { it.compressedUri }
                            val mimeType = results.first().let { result ->
                                when (result.mediaType) {
                                    MediaType.IMAGE -> "image/*"
                                    MediaType.VIDEO -> "video/*"
                                    MediaType.AUDIO -> "audio/*"
                                }
                            }
                            val intent = ShareUtil.createShareIntent(context, uris, mimeType)
                            context.startActivity(Intent.createChooser(intent, "Share compressed file"))
                        },
                        onSave = { results ->
                            results.forEach { result ->
                                val genericMimeType = when (result.mediaType) {
                                    MediaType.IMAGE -> "image/*"
                                    MediaType.VIDEO -> "video/*"
                                    MediaType.AUDIO -> "audio/*"
                                }
                                val exactMimeType = context.contentResolver.getType(result.compressedUri) ?: genericMimeType
                                val compressedName = FileUtil.getFileName(context, result.compressedUri) ?: ""
                                val extension = compressedName.substringAfterLast('.', "")
                                val extSuffix = if (extension.isNotEmpty()) ".$extension" else ""
                                
                                val originalName = FileUtil.getFileName(context, result.originalUri) ?: "smol_file"
                                val baseName = originalName.substringBeforeLast('.')
                                val fileName = "${baseName}_smol$extSuffix"
                                
                                ShareUtil.saveToDownloads(context, result.compressedUri, fileName, exactMimeType)
                            }
                            Toast.makeText(context, "Saved to Downloads/SMOL", Toast.LENGTH_SHORT).show()
                        },
                        onAttach = { results ->
                            val uris = results.map { it.compressedUri }
                            val resultIntent = Intent().apply {
                                if (uris.size == 1) {
                                    data = uris.first()
                                } else {
                                    val clipData = ClipData.newUri(context.contentResolver, "Compressed Files", uris.first())
                                    for (i in 1 until uris.size) {
                                        clipData.addItem(ClipData.Item(uris[i]))
                                    }
                                    this.clipData = clipData
                                }
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            (context as? Activity)?.let { activity ->
                                activity.setResult(Activity.RESULT_OK, resultIntent)
                                activity.finish()
                            }
                        }
                    )
                }

                is CompressUiState.Error -> {
                    ErrorContent(
                        state = state,
                        onRetry = { viewModel.retry() }
                    )
                }
            }
        }
    }
}
