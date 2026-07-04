package com.michaelsolati.smol.ui.compress

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
