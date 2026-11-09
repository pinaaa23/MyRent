package com.myrent.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.myrent.app.R

/**
 * Definisi rute navigasi dan properti menu bottom bar & sub-layar
 */
sealed class Screen(
    val route: String,
    @StringRes val titleResId: Int = 0,
    val icon: ImageVector? = null
) {
    // Rute Layar Otentikasi & Akun
    object Login : Screen("login")
    object Register : Screen("register")
    object VerifyEmail : Screen("verify_email")
    object ForgotPassword : Screen("forgot_password")
    object MainApp : Screen("main_app")
    object Settings : Screen("settings")
    object EditProfile : Screen("edit_profile")
    object CustomReceipt : Screen("custom_receipt")
    object WaTemplates : Screen("wa_templates")

    // Rute Bottom Navigation Menu (5 Menu)
    object Home : Screen("home", R.string.nav_home, Icons.Filled.Home)
    object Rental : Screen("rental", R.string.nav_rent, Icons.AutoMirrored.Filled.ReceiptLong)
    object Catalog : Screen("catalog", R.string.nav_catalog, Icons.Filled.Checkroom)
    object Customer : Screen("customer", R.string.nav_customer, Icons.Filled.Person)
    object Report : Screen("report", R.string.nav_report, Icons.Filled.BarChart)
}
