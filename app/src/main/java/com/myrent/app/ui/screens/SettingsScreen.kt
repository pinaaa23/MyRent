package com.myrent.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myrent.app.R
import com.myrent.app.data.foto.FotoRepository
import com.myrent.app.ui.components.AppBackground
import com.myrent.app.ui.pengaturan.KelolaKategoriDialog
import com.myrent.app.ui.pengaturan.SettingsViewModel
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.GoldBorderColor
import com.myrent.app.ui.theme.OnAccentGold
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Layar Pengaturan Aplikasi MyRent dengan Menu Upload QRIS Toko di Bawah Kustom Pesan Follow Up
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStoreProfile: () -> Unit = {},
    onNavigateToCustomReceipt: () -> Unit = {},
    onNavigateToWaTemplates: () -> Unit = {},
    onLogoutClick: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val storeProfile by settingsViewModel.storeProfile.collectAsState()
    val userDoc by settingsViewModel.userDocState.collectAsState()
    val userName by settingsViewModel.userName.collectAsState()
    val userNamaToko by settingsViewModel.userNamaToko.collectAsState()
    val userEmail by settingsViewModel.userEmail.collectAsState()

    var showKelolaKategoriDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showQrisUploadSheet by remember { mutableStateOf(false) }

    val fotoRepo = remember { FotoRepository() }
    val sheetState = rememberModalBottomSheetState()

    val displayFotoBase64 = storeProfile.fotoBase64.ifEmpty { userDoc.fotoProfilBase64 }
    val displayNamaToko = storeProfile.nama.ifEmpty { userNamaToko }

    val qrisPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val compressed = fotoRepo.compressUriToBase64(context, uri)
                if (compressed != null) {
                    settingsViewModel.saveQrisImage(context, compressed) {}
                }
            }
        }
    }

    AppBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_settings), color = TextPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Kartu Profil Pengguna & Nama Toko
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToStoreProfile() },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GoldBorderColor),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val avatarBitmap = remember(displayFotoBase64) {
                            if (displayFotoBase64.isNotEmpty()) fotoRepo.base64ToBitmap(displayFotoBase64) else null
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
                                    contentDescription = "Foto Toko",
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Store,
                                    contentDescription = "Toko",
                                    tint = AccentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = displayNamaToko.ifEmpty { "Nama toko belum diisi" },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (userName.isNotEmpty()) "Pemilik: $userName" else "Pemilik Toko",
                                fontSize = 13.sp,
                                color = AccentGold,
                                fontWeight = FontWeight.Medium
                            )
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

                // Menu 1: Profil Toko
                SettingsMenuItem(
                    title = "Profil Toko",
                    subtitle = "Atur nama, nomor telepon, dan alamat toko",
                    icon = Icons.Filled.Store,
                    onClick = onNavigateToStoreProfile
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Menu 2: Kelola Kategori
                SettingsMenuItem(
                    title = stringResource(R.string.menu_manage_category),
                    subtitle = "Tambah, ubah, atau hapus kategori barang",
                    icon = Icons.Filled.Category,
                    onClick = {
                        settingsViewModel.resetUiState()
                        showKelolaKategoriDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Menu 3: Kustom Nota
                SettingsMenuItem(
                    title = "Kustom Nota",
                    subtitle = "Atur judul, header, dan footer nota cetak",
                    icon = Icons.Filled.Receipt,
                    onClick = onNavigateToCustomReceipt
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Menu 4: Kustom Pesan Follow Up
                SettingsMenuItem(
                    title = "Kustom Pesan Follow Up",
                    subtitle = "Atur pesan WA pengingat pengembalian & nota",
                    icon = Icons.AutoMirrored.Filled.Message,
                    onClick = onNavigateToWaTemplates
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Menu 5: Upload QRIS Toko (Berada tepat di bawah Kustom Pesan Follow Up)
                SettingsMenuItem(
                    title = "Upload QRIS Toko",
                    subtitle = "Atur barcode QRIS pembayaran toko",
                    icon = Icons.Filled.QrCode2,
                    onClick = { showQrisUploadSheet = true }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Tombol Keluar (Logout)
                Button(
                    onClick = { showLogoutConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
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
    }

    // Modal BottomSheet Upload Barcode QRIS Toko
    if (showQrisUploadSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQrisUploadSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Upload Barcode QRIS Toko",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Unggah barcode QRIS resmi toko Anda untuk ditampilkan di layar pembayaran konsumen.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val qrisBitmap = remember(storeProfile.qrisBase64) {
                    if (storeProfile.qrisBase64.isNotEmpty()) fotoRepo.base64ToBitmap(storeProfile.qrisBase64) else null
                }

                if (qrisBitmap != null) {
                    Image(
                        bitmap = qrisBitmap.asImageBitmap(),
                        contentDescription = "QRIS Toko",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                qrisPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold)
                        ) {
                            Text("Ganti QRIS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                settingsViewModel.saveQrisImage(context, "") {
                                    showQrisUploadSheet = false
                                }
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B))
                        ) {
                            Text("Hapus", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            qrisPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = OnAccentGold)
                    ) {
                        Icon(Icons.Filled.QrCode2, contentDescription = null, tint = OnAccentGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unggah Barcode QRIS Toko", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnAccentGold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showKelolaKategoriDialog) {
        KelolaKategoriDialog(
            viewModel = settingsViewModel,
            onDismiss = { showKelolaKategoriDialog = false }
        )
    }

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
    subtitle: String = "",
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GoldBorderColor),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
