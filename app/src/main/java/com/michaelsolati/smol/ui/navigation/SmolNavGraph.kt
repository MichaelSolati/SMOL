package com.michaelsolati.smol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.michaelsolati.smol.ui.compress.CompressScreen
import com.michaelsolati.smol.ui.compress.CompressViewModel
import com.michaelsolati.smol.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Settings : Screen("settings")
    data object Compress : Screen("compress")
}

@Composable
fun SmolNavGraph(
    navController: NavHostController,
    startDestination: String,
    compressViewModel: CompressViewModel
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Settings.route) {
            SettingsScreen(
                onFilesSelected = { uris ->
                    compressViewModel.loadSharedFiles(uris, autoCompress = false)
                    navController.navigate(Screen.Compress.route)
                }
            )
        }
        composable(Screen.Compress.route) {
            CompressScreen(
                viewModel = compressViewModel,
                onBackClick = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(Screen.Compress.route) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
