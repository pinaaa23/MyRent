package com.myrent.app.ui.pengaturan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myrent.app.data.settings.DefaultStoreConfig
import com.myrent.app.data.settings.TemplateReplacer
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Layar Pengaturan Template Pesan WhatsApp
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WaTemplatesScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val storeConfig by settingsViewModel.storeConfig.collectAsState()
    val userDoc by settingsViewModel.userDocState.collectAsState()
    val uiState by settingsViewModel.uiState.collectAsState()
    val isLoading = uiState is SettingsUiState.Loading

    var tfvNota by remember(storeConfig) {
        mutableStateOf(TextFieldValue(storeConfig.templatePesanNota.ifEmpty { DefaultStoreConfig.TEMPLATE_NOTA_DEFAULT }))
    }
    var tfvH1 by remember(storeConfig) {
        mutableStateOf(TextFieldValue(storeConfig.templatePesanH1.ifEmpty { DefaultStoreConfig.TEMPLATE_H1_DEFAULT }))
    }
    var tfvH by remember(storeConfig) {
        mutableStateOf(TextFieldValue(storeConfig.templatePesanH.ifEmpty { DefaultStoreConfig.TEMPLATE_H_DEFAULT }))
    }

    var activeField by remember { mutableStateOf(1) } // 1: Nota, 2: H-1, 3: Hari H
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val availableChips = listOf(
        "{nama}" to "Nama Penyewa",
        "{barang}" to "Nama Barang",
        "{tanggal}" to "Tanggal",
        "{total}" to "Total Biaya",
        "{dp}" to "Uang Muka",
        "{sisa}" to "Sisa Bayar",
        "{invoice}" to "No Invoice",
        "{toko}" to "Nama Toko"
    )

    fun insertTagAtCursor(tfv: TextFieldValue, tag: String): TextFieldValue {
        val text = tfv.text
        val selection = tfv.selection
        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)

        val newText = text.replaceRange(start, end, tag)
        val newCursorPos = start + tag.length
        return TextFieldValue(text = newText, selection = TextRange(newCursorPos))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Template Pesan WA", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = "Info", tint = AccentGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ketuk chip variabel di bawah untuk menyisipkan ke posisi kursor:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableChips.forEach { (tag, label) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    when (activeField) {
                                        1 -> tfvNota = insertTagAtCursor(tfvNota, tag)
                                        2 -> tfvH1 = insertTagAtCursor(tfvH1, tag)
                                        3 -> tfvH = insertTagAtCursor(tfvH, tag)
                                    }
                                },
                                label = { Text("$tag ($label)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = AccentGold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. TEMPLATE PESAN NOTA
            Text("1. Template Pesan Nota Transaksi", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = tfvNota,
                onValueChange = {
                    tfvNota = it
                    activeField = 1
                    settingsViewModel.resetUiState()
                },
                minLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeField = 1 },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pratinjau Live Pesan Nota
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Pratinjau Pesan Nota:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = TemplateReplacer.replaceVariables(
                            template = tfvNota.text,
                            nama = "Fauzan",
                            barang = "Kamera Sony A6400 (1 unit)",
                            tanggal = "09 Nov 2026 s/d 10 Nov 2026 (1 Hari)",
                            total = "Rp150.000",
                            dp = "Rp50.000",
                            sisa = "Rp100.000",
                            invoice = "INV-20261109-01",
                            toko = userDoc.namaToko.ifEmpty { "Kusuma Rent" }
                        ),
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. TEMPLATE PESAN FOLLOW-UP H-1
            Text("2. Template Pesan Follow-Up H-1", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = tfvH1,
                onValueChange = {
                    tfvH1 = it
                    activeField = 2
                    settingsViewModel.resetUiState()
                },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeField = 2 },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pratinjau Live Pesan H-1
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Pratinjau Pesan H-1:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = TemplateReplacer.replaceVariables(
                            template = tfvH1.text,
                            nama = "Fauzan",
                            barang = "Kamera Sony A6400",
                            tanggal = "10 Nov 2026"
                        ),
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. TEMPLATE PESAN FOLLOW-UP HARI H
            Text("3. Template Pesan Follow-Up Hari H", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = tfvH,
                onValueChange = {
                    tfvH = it
                    activeField = 3
                    settingsViewModel.resetUiState()
                },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeField = 3 },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pratinjau Live Pesan Hari H
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Pratinjau Pesan Hari H:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = TemplateReplacer.replaceVariables(
                            template = tfvH.text,
                            nama = "Fauzan",
                            barang = "Kamera Sony A6400",
                            tanggal = "Hari Ini"
                        ),
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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

            // Tombol Simpan Template
            Button(
                onClick = {
                    settingsViewModel.saveWaTemplates(
                        templateNota = tfvNota.text,
                        templateH1 = tfvH1.text,
                        templateH = tfvH.text,
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
                    Text("Simpan Template WA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialog Konfirmasi Kembalikan ke Bawaan
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Kembalikan ke Bawaan?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin mengembalikan semua template pesan WhatsApp ke teks standar bawaan?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmDialog = false
                    tfvNota = TextFieldValue(DefaultStoreConfig.TEMPLATE_NOTA_DEFAULT)
                    tfvH1 = TextFieldValue(DefaultStoreConfig.TEMPLATE_H1_DEFAULT)
                    tfvH = TextFieldValue(DefaultStoreConfig.TEMPLATE_H_DEFAULT)
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
