package com.myrent.app.ui.pengaturan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myrent.app.data.foto.FotoRepository
import com.myrent.app.data.settings.DefaultStoreConfig
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Layar Pengaturan Kustom Nota Struk Kasir Thermal (toko/{uid}/pengaturan/nota)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomReceiptScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val notaConfig by settingsViewModel.notaConfig.collectAsState()
    val storeProfile by settingsViewModel.storeProfile.collectAsState()
    val userDoc by settingsViewModel.userDocState.collectAsState()
    val uiState by settingsViewModel.uiState.collectAsState()
    val isLoading = uiState is SettingsUiState.Loading

    var judulNotaInput by remember(notaConfig) {
        mutableStateOf(notaConfig.judulNota.ifEmpty { DefaultStoreConfig.NOTA_JUDUL_DEFAULT })
    }
    var pembukaInput by remember(notaConfig) {
        mutableStateOf(notaConfig.kalimatPembuka)
    }
    var catatanBawahInput by remember(notaConfig) {
        mutableStateOf(notaConfig.catatanBawah.ifEmpty { DefaultStoreConfig.NOTA_FOOTER_DEFAULT })
    }
    var flagTampilAlamat by remember(notaConfig) {
        mutableStateOf(notaConfig.flagTampilAlamat)
    }
    var flagTampilTelp by remember(notaConfig) {
        mutableStateOf(notaConfig.flagTampilTelp)
    }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val fotoRepo = remember { FotoRepository() }

    val namaTokoDisplay = storeProfile.nama.ifEmpty { userDoc.namaToko.ifEmpty { "Kusuma Rent" } }
    val alamatTokoDisplay = storeProfile.alamat.ifEmpty { userDoc.alamatToko }
    val noTelpTokoDisplay = storeProfile.noTelp.ifEmpty { userDoc.noWaToko }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kustom Nota Struk", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            if (uiState is SettingsUiState.Error) {
                Text(
                    text = (uiState as SettingsUiState.Error).message,
                    color = Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Info, contentDescription = "Info", tint = AccentGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Kop nota otomatis memakai Nama Toko, Alamat, dan No. Telepon dari Profil Toko.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Judul Nota (bawaan "NOTA", max 50 char)
            OutlinedTextField(
                value = judulNotaInput,
                onValueChange = { input ->
                    if (input.length <= 50) {
                        judulNotaInput = input
                        settingsViewModel.resetUiState()
                    }
                },
                label = { Text("Judul Nota (bawaan: NOTA)") },
                singleLine = true,
                supportingText = { Text("${judulNotaInput.length}/50", color = TextSecondary, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Kalimat Pembuka Kop (max 300 char)
            OutlinedTextField(
                value = pembukaInput,
                onValueChange = { input ->
                    if (input.length <= 300) {
                        pembukaInput = input
                        settingsViewModel.resetUiState()
                    }
                },
                label = { Text("Kalimat Pembuka Kop (opsional)") },
                placeholder = { Text("misal: Sewa Alat & Kamera Termurah") },
                supportingText = { Text("${pembukaInput.length}/300", color = TextSecondary, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Catatan Bawah (multiline, max 500 char)
            OutlinedTextField(
                value = catatanBawahInput,
                onValueChange = { input ->
                    if (input.length <= 500) {
                        catatanBawahInput = input
                        settingsViewModel.resetUiState()
                    }
                },
                label = { Text("Catatan Bawah Struk (Teks multi-baris)") },
                minLines = 3,
                supportingText = { Text("${catatanBawahInput.length}/500", color = TextSecondary, fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Saklar 1: Tampilkan Alamat Toko
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tampilkan Alamat Toko di Kop", color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = flagTampilAlamat,
                        onCheckedChange = { flagTampilAlamat = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = AccentGold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Saklar 2: Tampilkan No Telepon Toko
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tampilkan No. Telepon Toko di Kop", color = TextPrimary, fontSize = 14.sp)
                    Switch(
                        checked = flagTampilTelp,
                        onCheckedChange = { flagTampilTelp = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = AccentGold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // PRATINJAU STRUK KASIR
            Text("Pratinjau Hasil Struk Kasir:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val displayFotoBase64 = storeProfile.fotoBase64.ifEmpty { userDoc.fotoProfilBase64 }
                    val bitmap = remember(displayFotoBase64) {
                        if (displayFotoBase64.isNotEmpty()) fotoRepo.base64ToBitmap(displayFotoBase64) else null
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Logo Struk",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Nama Toko
                    Text(
                        text = namaTokoDisplay.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )

                    // Kalimat Pembuka Kop
                    if (pembukaInput.trim().isNotEmpty()) {
                        Text(
                            text = pembukaInput.trim(),
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Alamat & WA Toko berdasarkan Switch
                    if (flagTampilTelp && noTelpTokoDisplay.isNotEmpty()) {
                        Text(
                            text = "Telp/WA: $noTelpTokoDisplay",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }
                    if (flagTampilAlamat && alamatTokoDisplay.isNotEmpty()) {
                        Text(
                            text = alamatTokoDisplay,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text("----------------------------------------", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)

                    // Judul Nota & Rincian Contoh
                    Text(
                        text = (judulNotaInput.ifEmpty { "NOTA" }).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("No. Nota: NOTA-20261109-01", fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                        Text("LUNAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kamera Sony A6400 (1 unit)", fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                        Text("Rp150.000", fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                    }

                    Text("----------------------------------------", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)

                    // Catatan Bawah
                    if (catatanBawahInput.trim().isNotEmpty()) {
                        Text(
                            text = catatanBawahInput.trim(),
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol Kembalikan ke Bawaan
            OutlinedButton(
                onClick = { showResetConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold)
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kembalikan ke Bawaan", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol Simpan Nota (50dp)
            Button(
                onClick = {
                    settingsViewModel.saveFullNotaConfig(
                        judulNota = judulNotaInput,
                        kalimatPembuka = pembukaInput,
                        catatanBawah = catatanBawahInput,
                        flagTampilAlamat = flagTampilAlamat,
                        flagTampilTelp = flagTampilTelp,
                        onSuccess = onBack
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkBackground)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DarkBackground, strokeWidth = 2.dp)
                } else {
                    Text("Simpan Nota", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Kembalikan ke Bawaan?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin mengembalikan pengaturan nota ke format bawaan?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmDialog = false
                    judulNotaInput = DefaultStoreConfig.NOTA_JUDUL_DEFAULT
                    pembukaInput = ""
                    catatanBawahInput = DefaultStoreConfig.NOTA_FOOTER_DEFAULT
                    flagTampilAlamat = true
                    flagTampilTelp = true
                }) {
                    Text("Ya, Kembalikan", color = AccentGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}
