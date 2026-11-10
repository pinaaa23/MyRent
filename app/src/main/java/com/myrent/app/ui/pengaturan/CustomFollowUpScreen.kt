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
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
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
 * Layar Pengaturan Kustom Pesan Follow Up WhatsApp (toko/{uid}/pengaturan/nota)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomFollowUpScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val notaConfig by settingsViewModel.notaConfig.collectAsState()
    val storeProfile by settingsViewModel.storeProfile.collectAsState()
    val userDoc by settingsViewModel.userDocState.collectAsState()
    val uiState by settingsViewModel.uiState.collectAsState()
    val isLoading = uiState is SettingsUiState.Loading

    var tfvH1 by remember(notaConfig) {
        mutableStateOf(TextFieldValue(notaConfig.templateFollowUpH1.ifEmpty { DefaultStoreConfig.TEMPLATE_H1_DEFAULT }))
    }
    var tfvHariH by remember(notaConfig) {
        mutableStateOf(TextFieldValue(notaConfig.templateFollowUpHariH.ifEmpty { DefaultStoreConfig.TEMPLATE_H_DEFAULT }))
    }
    var tfvTelat by remember(notaConfig) {
        mutableStateOf(TextFieldValue(notaConfig.templateFollowUpTelat.ifEmpty { DefaultStoreConfig.TEMPLATE_TELAT_DEFAULT }))
    }

    var activeField by remember { mutableStateOf(1) } // 1: H-1, 2: Hari H, 3: Telat
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val namaTokoDisplay = storeProfile.nama.ifEmpty { userDoc.namaToko.ifEmpty { "Kusuma Rent" } }
    val noTelpDisplay = storeProfile.noTelp.ifEmpty { userDoc.noWaToko }

    val availableChips = listOf(
        "{nama_penyewa}" to "Nama Penyewa",
        "{barang}" to "Nama Barang",
        "{tanggal_sewa}" to "Tanggal Sewa",
        "{tanggal_kembali}" to "Tanggal Kembali",
        "{hari_telat}" to "Hari Telat (Khusus Telat)",
        "{nama_toko}" to "Nama Toko",
        "{telp_toko}" to "No Telp Toko"
    )

    fun insertTagAtCursor(tfv: TextFieldValue, tag: String): TextFieldValue {
        val text = tfv.text
        if (text.length + tag.length > 1000) return tfv

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
                title = { Text("Kustom Pesan Follow Up", color = TextPrimary, fontWeight = FontWeight.Bold) },
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

            // Petunjuk Penyisipan Variabel Chip
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = "Info", tint = AccentGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ketuk chip variabel di bawah untuk menyisipkan ke posisi kursor (maks 1000 karakter):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                                        1 -> tfvH1 = insertTagAtCursor(tfvH1, tag)
                                        2 -> tfvHariH = insertTagAtCursor(tfvHariH, tag)
                                        3 -> tfvTelat = insertTagAtCursor(tfvTelat, tag)
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

            // 1. TEMPLATE FOLLOW-UP H-1
            Text("1. Template Pesan Follow-Up H-1", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = tfvH1,
                onValueChange = { input ->
                    if (input.text.length <= 1000) {
                        tfvH1 = input
                        activeField = 1
                        settingsViewModel.resetUiState()
                    }
                },
                minLines = 3,
                supportingText = { Text("${tfvH1.text.length}/1000", color = TextSecondary, fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeField = 1 },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            // Warning jika ada variabel tak dikenal
            val unknownH1 = remember(tfvH1.text) { TemplateReplacer.findUnknownVariables(tfvH1.text) }
            if (unknownH1.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Warning, contentDescription = "Peringatan", tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Peringatan: Variabel {${unknownH1.joinToString()}} tidak dikenal dan akan dibiarkan apa adanya.", color = Color(0xFFFFB74D), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Pratinjau H-1
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
                            barang = "Kamera Sony A6400 (1 unit)",
                            tanggalSewa = "09 Nov 2026",
                            tanggal = "10 Nov 2026",
                            toko = namaTokoDisplay,
                            telpToko = noTelpDisplay
                        ),
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. TEMPLATE FOLLOW-UP HARI H
            Text("2. Template Pesan Follow-Up Hari H", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = tfvHariH,
                onValueChange = { input ->
                    if (input.text.length <= 1000) {
                        tfvHariH = input
                        activeField = 2
                        settingsViewModel.resetUiState()
                    }
                },
                minLines = 3,
                supportingText = { Text("${tfvHariH.text.length}/1000", color = TextSecondary, fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeField = 2 },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            val unknownHariH = remember(tfvHariH.text) { TemplateReplacer.findUnknownVariables(tfvHariH.text) }
            if (unknownHariH.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Warning, contentDescription = "Peringatan", tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Peringatan: Variabel {${unknownHariH.joinToString()}} tidak dikenal.", color = Color(0xFFFFB74D), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

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
                            template = tfvHariH.text,
                            nama = "Fauzan",
                            barang = "Kamera Sony A6400 (1 unit)",
                            tanggalSewa = "09 Nov 2026",
                            tanggal = "Hari Ini (10 Nov 2026)",
                            toko = namaTokoDisplay,
                            telpToko = noTelpDisplay
                        ),
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. TEMPLATE FOLLOW-UP TELAT
            Text("3. Template Pesan Follow-Up Telat Pengembalian", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = tfvTelat,
                onValueChange = { input ->
                    if (input.text.length <= 1000) {
                        tfvTelat = input
                        activeField = 3
                        settingsViewModel.resetUiState()
                    }
                },
                minLines = 3,
                supportingText = { Text("${tfvTelat.text.length}/1000", color = TextSecondary, fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { activeField = 3 },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            val unknownTelat = remember(tfvTelat.text) { TemplateReplacer.findUnknownVariables(tfvTelat.text) }
            if (unknownTelat.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Warning, contentDescription = "Peringatan", tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Peringatan: Variabel {${unknownTelat.joinToString()}} tidak dikenal.", color = Color(0xFFFFB74D), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Pratinjau Pesan Telat:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = TemplateReplacer.replaceVariables(
                            template = tfvTelat.text,
                            nama = "Fauzan",
                            barang = "Kamera Sony A6400 (1 unit)",
                            tanggalSewa = "08 Nov 2026",
                            tanggal = "09 Nov 2026",
                            toko = namaTokoDisplay,
                            telpToko = noTelpDisplay,
                            telat = "2"
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

            // Tombol Simpan Template WA (50dp)
            Button(
                onClick = {
                    settingsViewModel.saveWaFollowUpTemplates(
                        templateH1 = tfvH1.text,
                        templateHariH = tfvHariH.text,
                        templateTelat = tfvTelat.text,
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
                    Text("Simpan Pesan Follow Up", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Kembalikan ke Bawaan?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin mengembalikan ketiga template pesan follow-up ke teks standar bawaan?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmDialog = false
                    tfvH1 = TextFieldValue(DefaultStoreConfig.TEMPLATE_H1_DEFAULT)
                    tfvHariH = TextFieldValue(DefaultStoreConfig.TEMPLATE_H_DEFAULT)
                    tfvTelat = TextFieldValue(DefaultStoreConfig.TEMPLATE_TELAT_DEFAULT)
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
