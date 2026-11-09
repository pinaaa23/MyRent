package com.myrent.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.myrent.app.ui.pengaturan.CustomReceiptScreen
import com.myrent.app.ui.pengaturan.EditProfileScreen
import com.myrent.app.ui.pengaturan.EditProfileViewModel
import com.myrent.app.ui.pengaturan.SettingsViewModel
import com.myrent.app.ui.pengaturan.WaTemplatesScreen
import com.myrent.app.ui.screens.CatalogScreen
import com.myrent.app.ui.screens.CustomerScreen
import com.myrent.app.ui.screens.HomeScreen
import com.myrent.app.ui.screens.RentalScreen
import com.myrent.app.ui.screens.ReportScreen
import com.myrent.app.ui.screens.SettingsScreen

/**
 * Mengatur grafik navigasi (NavGraph) 5 menu utama aplikasi, Settings, Edit Profil, Kustom Nota, & Template WA
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Rental.route) {
            RentalScreen()
        }
        composable(Screen.Catalog.route) {
            CatalogScreen()
        }
        composable(Screen.Customer.route) {
            CustomerScreen()
        }
        composable(Screen.Report.route) {
            ReportScreen()
        }
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToCustomReceipt = {
                    navController.navigate(Screen.CustomReceipt.route)
                },
                onNavigateToWaTemplates = {
                    navController.navigate(Screen.WaTemplates.route)
                },
                onLogoutClick = onLogoutClick,
                settingsViewModel = settingsViewModel
            )
        }
        composable(Screen.EditProfile.route) {
            val editProfileViewModel: EditProfileViewModel = viewModel()
            EditProfileScreen(
                viewModel = editProfileViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.CustomReceipt.route) {
            val settingsViewModel: SettingsViewModel = viewModel()
            CustomReceiptScreen(
                settingsViewModel = settingsViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.WaTemplates.route) {
            val settingsViewModel: SettingsViewModel = viewModel()
            WaTemplatesScreen(
                settingsViewModel = settingsViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
