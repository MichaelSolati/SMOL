package com.michaelsolati.smol.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.michaelsolati.smol.ui.components.SmolTopAppBar

enum class SettingsTab(val title: String, val icon: ImageVector) {
    IMAGES("Images", Icons.Default.Image),
    VIDEO("Video", Icons.Default.Videocam),
    AUDIO("Audio", Icons.Default.AudioFile),
    SETTINGS("Settings", Icons.Default.Settings)
}

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

    val topBarTitle = when (selectedTab) {
        0 -> "Image Settings"
        1 -> "Video Settings"
        2 -> "Audio Settings"
        else -> "App Settings"
    }

    Scaffold(
        topBar = {
            SmolTopAppBar(title = topBarTitle)
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
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
            // Only show FAB when on configuration tabs
            if (selectedTab < 3) {
                FloatingActionButton(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("image/*", "video/*", "audio/*"))
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Select files")
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> ImageSettingsScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            1 -> VideoSettingsScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            2 -> AudioSettingsScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            3 -> GeneralSettingsScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
        }
    }
}
