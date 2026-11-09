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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
 * Layar Pengaturan Kustom Nota Struk Thermal Kasir
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomReceiptScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val storeConfig by settingsViewModel.storeConfig.collectAsState()
    val userDoc by settingsViewModel.userDocState.collectAsState()
    val uiState by settingsViewModel.uiState.collectAsState()
    val isLoading = uiState is SettingsUiState.Loading

    var headerInput by remember(storeConfig) {
        mutableStateOf(storeConfig.notaHeader.ifEmpty { DefaultStoreConfig.NOTA_HEADER_DEFAULT })
    }
    var footerInput by remember(storeConfig) {
        mutableStateOf(storeConfig.notaFooter.ifEmpty { DefaultStoreConfig.NOTA_FOOTER_DEFAULT })
    }

    val scrollState = rememberScrollState()
    val fotoRepo = remember { FotoRepository() }

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
            // Informasi Sumber Alamat & WA Toko
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
                        text = "Nama Toko, Alamat, dan No WA Kop Struk diambil otomatis dari Edit Profil Toko.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Teks Header Kop Struk
            OutlinedTextField(
                value = headerInput,
                onValueChange = {
                    headerInput = it
                    settingsViewModel.resetUiState()
                },
                label = { Text("Teks Header Kop (di bawah Nama Toko)") },
                placeholder = { Text("misal: Sewa Alat & Kamera Termurah") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Input Teks Footer Struk
            OutlinedTextField(
                value = footerInput,
                onValueChange = {
                    footerInput = it
                    settingsViewModel.resetUiState()
                },
                label = { Text("Teks Footer (Bagian Bawah Struk)") },
                placeholder = { Text("misal: Terima kasih telah menyewa di toko kami!") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

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
                    // Logo Toko (jika ada)
                    val bitmap = remember(userDoc.fotoProfilBase64) {
                        if (userDoc.fotoProfilBase64.isNotEmpty()) fotoRepo.base64ToBitmap(userDoc.fotoProfilBase64) else null
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
                        text = userDoc.namaToko.ifEmpty { "MYRENT STORE" }.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )

                    // Header Teks
                    if (headerInput.trim().isNotEmpty()) {
                        Text(
                            text = headerInput.trim(),
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Alamat & No WA
                    val alamatWaText = buildString {
                        if (userDoc.alamatToko.isNotEmpty()) append(userDoc.alamatToko)
                        if (userDoc.noWaToko.isNotEmpty()) {
                            if (isNotEmpty()) append(" • ")
                            append("WA: ${userDoc.noWaToko}")
                        }
                    }
                    if (alamatWaText.isNotEmpty()) {
                        Text(
                            text = alamatWaText,
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text("----------------------------------------", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)

                    // Contoh Rincian
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("No. Invoice: INV-20261109-01", fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                        Text("LUNAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kamera Sony A6400 (1 unit)", fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                        Text("Rp150.000", fontSize = 10.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                    }

                    Text("----------------------------------------", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)

                    // Footer Teks
                    if (footerInput.trim().isNotEmpty()) {
                        Text(
                            text = footerInput.trim(),
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Simpan (50dp)
            Button(
                onClick = {
                    settingsViewModel.saveNotaConfig(
                        header = headerInput,
                        footer = footerInput,
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
                    Text("Simpan Kustom Nota", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
