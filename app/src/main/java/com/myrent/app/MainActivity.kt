package com.myrent.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myrent.app.data.settings.LocalStoreProfileCache
import com.myrent.app.ui.auth.AuthViewModel
import com.myrent.app.ui.auth.ForgotPasswordScreen
import com.myrent.app.ui.auth.LoginScreen
import com.myrent.app.ui.auth.RegisterScreen
import com.myrent.app.ui.auth.SplashScreen
import com.myrent.app.ui.auth.VerifyEmailScreen
import com.myrent.app.ui.navigation.AppNavGraph
import com.myrent.app.ui.navigation.CustomBottomNavigation
import com.myrent.app.ui.navigation.Screen
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.MyRentTheme
import kotlinx.coroutines.launch

/**
 * Activity Utama Aplikasi MyRent dengan System & In-App Splash Screen
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyRentTheme {
                RootAppNavigation()
            }
        }
    }
}

/**
 * Pengatur Navigasi Utama Aplikasi (Splash In-App, Auth, Verify Email, dan Main App)
 */
@Composable
fun RootAppNavigation(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                viewModel = authViewModel,
                onNavigateToMain = {
                    navController.navigate(Screen.MainApp.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToVerifyEmail = {
                    navController.navigate(Screen.VerifyEmail.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    authViewModel.resetUiState()
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    authViewModel.resetUiState()
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onLoginSuccess = { isVerified ->
                    if (isVerified) {
                        navController.navigate(Screen.MainApp.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.VerifyEmail.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    authViewModel.resetUiState()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.VerifyEmail.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.VerifyEmail.route) {
            VerifyEmailScreen(
                viewModel = authViewModel,
                onVerifiedSuccess = {
                    navController.navigate(Screen.MainApp.route) {
                        popUpTo(Screen.VerifyEmail.route) { inclusive = true }
                    }
                },
                onChangeAccount = {
                    coroutineScope.launch {
                        LocalStoreProfileCache.clearCache(context)
                    }
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.VerifyEmail.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onBackToLogin = {
                    authViewModel.resetUiState()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.MainApp.route) {
            MainScreen(
                onLogout = {
                    coroutineScope.launch {
                        LocalStoreProfileCache.clearCache(context)
                    }
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.MainApp.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Layar Kontainer Aplikasi Utama (5 Menu Bottom Nav & Settings)
 */
@Composable
fun MainScreen(
    onLogout: () -> Unit = {}
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground,
        bottomBar = {
            if (currentRoute != Screen.Settings.route &&
                currentRoute != Screen.StoreProfile.route &&
                currentRoute != Screen.EditProfile.route &&
                currentRoute != Screen.CustomReceipt.route &&
                currentRoute != Screen.CustomFollowUp.route &&
                currentRoute != Screen.WaTemplates.route) {
                CustomBottomNavigation(
                    currentRoute = currentRoute,
                    onItemSelected = { screen ->
                        bottomNavController.navigate(screen.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        AppNavGraph(
            navController = bottomNavController,
            onLogoutClick = onLogout,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
