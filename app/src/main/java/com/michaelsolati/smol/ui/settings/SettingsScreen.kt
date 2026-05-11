package com.michaelsolati.smol.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel

enum class SettingsTab(val title: String, val icon: ImageVector) {
    IMAGES("Images", Icons.Default.Image),
    VIDEO("Video", Icons.Default.Videocam),
    AUDIO("Audio", Icons.Default.AudioFile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onFilesSelected: (List<Uri>) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onFilesSelected(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SMOL") })
        },
        bottomBar = {
            NavigationBar {
                SettingsTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    filePickerLauncher.launch(arrayOf("image/*", "video/*", "audio/*"))
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Select files")
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> ImageSettingsScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            1 -> VideoSettingsScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            2 -> AudioSettingsScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
        }
    }
}
