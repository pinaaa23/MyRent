package com.myrent.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myrent.app.R
import com.myrent.app.data.foto.FotoRepository
import com.myrent.app.ui.pengaturan.KelolaKategoriDialog
import com.myrent.app.ui.pengaturan.SettingsViewModel
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Layar Pengaturan Aplikasi MyRent (1 Akun = 1 Toko Mandiri)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToCustomReceipt: () -> Unit = {},
    onNavigateToWaTemplates: () -> Unit = {},
    onLogoutClick: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val userDoc by settingsViewModel.userDocState.collectAsState()
    val userName by settingsViewModel.userName.collectAsState()
    val userNamaToko by settingsViewModel.userNamaToko.collectAsState()
    val userEmail by settingsViewModel.userEmail.collectAsState()

    var showKelolaKategoriDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    val fotoRepo = remember { FotoRepository() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings), color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Kartu Profil Pengguna & Nama Toko (Dapat Diketuk -> Edit Profil)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToEditProfile() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val avatarBitmap = remember(userDoc.fotoProfilBase64) {
                        if (userDoc.fotoProfilBase64.isNotEmpty()) fotoRepo.base64ToBitmap(userDoc.fotoProfilBase64) else null
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap.asImageBitmap(),
                                contentDescription = "Logo Toko",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profil",
                                tint = AccentGold,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (userName.isNotEmpty()) userName else "Pemilik Toko",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Store,
                                contentDescription = "Toko",
                                tint = AccentGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Toko: $userNamaToko",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentGold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userEmail,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Menu 1: Kelola Kategori
            SettingsMenuItem(
                title = stringResource(R.string.menu_manage_category),
                icon = Icons.Filled.Category,
                onClick = {
                    settingsViewModel.resetUiState()
                    showKelolaKategoriDialog = true
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Menu 2: Kustom Nota
            SettingsMenuItem(
                title = "Kustom Nota",
                icon = Icons.Filled.Receipt,
                onClick = onNavigateToCustomReceipt
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Menu 3: Template Pesan WA
            SettingsMenuItem(
                title = "Template Pesan WA",
                icon = Icons.AutoMirrored.Filled.Message,
                onClick = onNavigateToWaTemplates
            )

            Spacer(modifier = Modifier.weight(1f))

            // Tombol Keluar (Logout)
            Button(
                onClick = { showLogoutConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = Color(0xFFFF6B6B)
                )
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Keluar")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_logout),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialog Kelola Kategori
    if (showKelolaKategoriDialog) {
        KelolaKategoriDialog(
            viewModel = settingsViewModel,
            onDismiss = { showKelolaKategoriDialog = false }
        )
    }

    // Dialog Konfirmasi Keluar (Logout)
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text(stringResource(R.string.confirm_logout_title), color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.confirm_logout_msg), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirmDialog = false
                    settingsViewModel.logout()
                    onLogoutClick()
                }) {
                    Text(stringResource(R.string.btn_confirm), color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun SettingsMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = AccentGold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}
