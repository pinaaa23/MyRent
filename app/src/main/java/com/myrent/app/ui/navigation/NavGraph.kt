package com.myrent.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.myrent.app.ui.screens.CatalogScreen
import com.myrent.app.ui.screens.HomeScreen
import com.myrent.app.ui.screens.RentalScreen
import com.myrent.app.ui.screens.ReportScreen
import com.myrent.app.ui.screens.SettingsScreen

/**
 * Mengatur grafik navigasi (NavGraph) 4 menu utama aplikasi dan Layar Pengaturan
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
        composable(Screen.Report.route) {
            ReportScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogoutClick = onLogoutClick
            )
        }
    }
}
