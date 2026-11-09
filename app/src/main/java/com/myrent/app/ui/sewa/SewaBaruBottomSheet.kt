package com.myrent.app.ui.sewa

import android.app.DatePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.R
import com.myrent.app.data.customer.normalizeNoWa
import com.myrent.app.data.foto.FotoRepository
import com.myrent.app.data.items.Item
import com.myrent.app.data.rentals.Rental
import com.myrent.app.ui.screens.formatDate
import com.myrent.app.ui.screens.formatRupiah
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary
import java.util.Calendar
import java.util.concurrent.TimeUnit

fun calculateDays(startMs: Long, endMs: Long): Int {
    val diff = endMs - startMs
    val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
    return if (days <= 0) 1 else days
}

/**
 * LANGKAH 2 - Bottom Sheet Formulir "Sewa Baru" dengan Saran Otomatis Penyewa & Format Rupiah
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SewaBaruBottomSheet(
    sheetState: SheetState,
    viewModel: RentalViewModel,
    selectedItem: Item?,
    rentalToEdit: Rental? = null,
    fotoRepository: FotoRepository = FotoRepository(),
    onChangeItemRequested: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = rentalToEdit != null
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is RentalUiState.Loading

    val customerList by viewModel.customerList.collectAsState()

    val userDoc by viewModel.userDocState.collectAsState()
    val kasirNama by viewModel.kasirNama.collectAsState()

    var namaPenyewa by remember { mutableStateOf(rentalToEdit?.namaPenyewa ?: "") }
    var noWa by remember { mutableStateOf(rentalToEdit?.noWa ?: "") }

    var showNamaSuggestions by remember { mutableStateOf(false) }
    var showWaSuggestions by remember { mutableStateOf(false) }

    val calStart = remember { Calendar.getInstance().apply { timeInMillis = rentalToEdit?.tanggalMulai ?: System.currentTimeMillis() } }
    val calEnd = remember { Calendar.getInstance().apply { timeInMillis = rentalToEdit?.tanggalKembali ?: (System.currentTimeMillis() + 86400000L) } }

    var tglMulaiMs by remember { mutableStateOf(calStart.timeInMillis) }
    var tglKembaliMs by remember { mutableStateOf(calEnd.timeInMillis) }

    var lamaHari by remember { mutableStateOf(calculateDays(tglMulaiMs, tglKembaliMs)) }
    var jumlahText by remember { mutableStateOf(rentalToEdit?.jumlah?.toString() ?: "1") }

    var hargaTotalText by remember(selectedItem, lamaHari, jumlahText, rentalToEdit) {
        mutableStateOf(
            if (rentalToEdit != null) {
                rentalToEdit.hargaTotal.toString()
            } else if (selectedItem != null) {
                val jml = jumlahText.toIntOrNull() ?: 1
                (selectedItem.hargaSewa * lamaHari.toLong() * jml.toLong()).toString()
            } else "0"
        )
    }

    var dpText by remember { mutableStateOf(rentalToEdit?.dp?.toString() ?: "0") }
    var catatan by remember { mutableStateOf(rentalToEdit?.catatan ?: "") }

    var showPaymentScreen by remember { mutableStateOf(false) }
    var createdRentalForNota by remember { mutableStateOf<Rental?>(null) }

    val totalLong = hargaTotalText.toLongOrNull() ?: 0L
    val dpLong = dpText.toLongOrNull() ?: 0L
    val sisaLong = if (totalLong > dpLong) totalLong - dpLong else 0L

    val scrollState = rememberScrollState()

    // Filter Saran Nama Penyewa (Maksimal 5 Unik)
    val namaSuggestions = remember(namaPenyewa, customerList, showNamaSuggestions) {
        if (showNamaSuggestions && namaPenyewa.trim().isNotEmpty()) {
            customerList
                .filter { it.nama.contains(namaPenyewa, ignoreCase = true) }
                .distinctBy { normalizeNoWa(it.noWa) }
                .take(5)
        } else emptyList()
    }

    // Filter Saran Nomor WA Penyewa (Maksimal 5 Unik)
    val waSuggestions = remember(noWa, customerList, showWaSuggestions) {
        if (showWaSuggestions && noWa.trim().isNotEmpty()) {
            val normInput = normalizeNoWa(noWa)
            customerList
                .filter { normalizeNoWa(it.noWa).contains(normInput) || it.noWa.contains(noWa) }
                .distinctBy { normalizeNoWa(it.noWa) }
                .take(5)
        } else emptyList()
    }

    // 1. Tampilkan Nota Transaksi jika transaksi selesai
    if (createdRentalForNota != null) {
        NotaTransaksiDialog(
            rental = createdRentalForNota!!,
            kasirNama = kasirNama,
            cabangNama = userDoc.namaToko,
            cabangAlamat = userDoc.alamatToko,
            cabangNoWa = userDoc.noWaToko,
            viewModel = viewModel,
            onClose = {
                createdRentalForNota = null
                onDismiss()
            }
        )
        return
    }

    // 2. Tampilkan Layar Pembayaran Full-Screen
    if (showPaymentScreen) {
        val jmlInt = jumlahText.toIntOrNull() ?: 1
        PembayaranScreen(
            totalBayar = totalLong,
            namaPenyewa = namaPenyewa,
            namaBarang = selectedItem?.nama ?: (rentalToEdit?.namaBarang ?: "Barang Sewa"),
            jumlahUnit = jmlInt,
            cabangNama = userDoc.namaToko.ifEmpty { "MYRENT STORE" },
            onBack = { showPaymentScreen = false },
            onKonfirmasiPembayaran = { metode ->
                showPaymentScreen = false
                if (isEditMode) {
                    viewModel.updateSewa(
                        rental = rentalToEdit!!,
                        namaPenyewa = namaPenyewa,
                        noWa = noWa,
                        tanggalMulai = tglMulaiMs,
                        tanggalKembali = tglKembaliMs,
                        lamaHari = lamaHari,
                        jumlah = jmlInt,
                        hargaTotal = totalLong,
                        dp = dpLong,
                        metodePembayaran = metode,
                        catatan = catatan,
                        onSuccess = onDismiss
                    )
                } else {
                    viewModel.prosesPembayaranDanSimpanSewa(
                        selectedItem = selectedItem,
                        namaPenyewa = namaPenyewa,
                        noWa = noWa,
                        tanggalMulai = tglMulaiMs,
                        tanggalKembali = tglKembaliMs,
                        lamaHari = lamaHari,
                        jumlah = jmlInt,
                        hargaTotal = totalLong,
                        dp = dpLong,
                        metodePembayaran = metode,
                        catatan = catatan,
                        onSuccess = { savedRental ->
                            createdRentalForNota = savedRental
                        }
                    )
                }
            }
        )
        return
    }

    // 3. Form Utama Input Data Sewa
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEditMode) stringResource(R.string.btn_edit_rental) else stringResource(R.string.title_add_rental),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState is RentalUiState.Error) {
                Text(
                    text = (uiState as RentalUiState.Error).message,
                    color = Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // RINGKASAN BARANG TERPILIH (Langkah 2 - Tanpa Dropdown)
            if (selectedItem != null) {
                val bitmap = remember(selectedItem.fotoBase64) {
                    if (selectedItem.fotoBase64.isNotEmpty()) fotoRepository.base64ToBitmap(selectedItem.fotoBase64) else null
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = selectedItem.nama,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Checkroom,
                                    contentDescription = selectedItem.nama,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedItem.nama,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${selectedItem.kategori} • ${formatRupiah(selectedItem.hargaSewa)}/unit",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tersedia: ${selectedItem.stokTersedia} unit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        }

                        if (!isEditMode) {
                            TextButton(onClick = onChangeItemRequested) {
                                Text("Ganti", color = AccentGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (rentalToEdit != null) {
                OutlinedTextField(
                    value = rentalToEdit.namaBarang,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Barang Disewa") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = DarkSurfaceVariant,
                        disabledLabelColor = AccentGold,
                        disabledTextColor = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nama Penyewa dengan Saran Otomatis
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = namaPenyewa,
                    onValueChange = {
                        namaPenyewa = it
                        showNamaSuggestions = true
                        showWaSuggestions = false
                        viewModel.resetUiState()
                    },
                    label = { Text(stringResource(R.string.label_tenant_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )

                // Kartu Daftar Saran Nama
                if (namaSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            namaSuggestions.forEach { customer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            namaPenyewa = customer.nama
                                            noWa = customer.noWa
                                            showNamaSuggestions = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = "User", tint = AccentGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(customer.nama, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("(${customer.noWa})", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nomor WA Penyewa dengan Saran Otomatis
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = noWa,
                    onValueChange = { input ->
                        noWa = input.filter { it.isDigit() || it == '+' }
                        showWaSuggestions = true
                        showNamaSuggestions = false
                        viewModel.resetUiState()
                    },
                    label = { Text(stringResource(R.string.label_tenant_phone)) },
                    placeholder = { Text("08123456789") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )

                // Kartu Daftar Saran Nomor WA
                if (waSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            waSuggestions.forEach { customer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            namaPenyewa = customer.nama
                                            noWa = customer.noWa
                                            showWaSuggestions = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = "User", tint = AccentGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(customer.noWa, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("(${customer.nama})", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Baris Tanggal Mulai & Tanggal Kembali
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = formatDate(tglMulaiMs),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_start_date)) },
                        trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = "Tgl Mulai", tint = AccentGold, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = tglMulaiMs }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                        tglMulaiMs = newCal.timeInMillis
                                        lamaHari = calculateDays(tglMulaiMs, tglKembaliMs)
                                        val jmlInt = jumlahText.toIntOrNull() ?: 1
                                        selectedItem?.let { hargaTotalText = (it.hargaSewa * lamaHari.toLong() * jmlInt.toLong()).toString() }
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = formatDate(tglKembaliMs),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_end_date)) },
                        trailingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = "Tgl Kembali", tint = AccentGold, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = tglKembaliMs }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                        tglKembaliMs = newCal.timeInMillis
                                        lamaHari = calculateDays(tglMulaiMs, tglKembaliMs)
                                        val jmlInt = jumlahText.toIntOrNull() ?: 1
                                        selectedItem?.let { hargaTotalText = (it.hargaSewa * lamaHari.toLong() * jmlInt.toLong()).toString() }
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lama Hari & Jumlah Disewa
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = "$lamaHari Hari",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text(stringResource(R.string.label_duration)) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = DarkSurfaceVariant, disabledTextColor = TextPrimary, disabledLabelColor = TextSecondary)
                )

                OutlinedTextField(
                    value = jumlahText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        jumlahText = filtered
                        val jmlInt = filtered.toIntOrNull() ?: 1
                        selectedItem?.let { hargaTotalText = (it.hargaSewa * lamaHari.toLong() * jmlInt.toLong()).toString() }
                        viewModel.resetUiState()
                    },
                    label = { Text(stringResource(R.string.label_rental_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Total Biaya & DP dengan Format Rp
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formatRupiah(totalLong),
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        hargaTotalText = filtered
                        viewModel.resetUiState()
                    },
                    label = { Text(stringResource(R.string.label_total_price)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                OutlinedTextField(
                    value = formatRupiah(dpLong),
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        dpText = filtered
                        viewModel.resetUiState()
                    },
                    label = { Text(stringResource(R.string.label_dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sisa Pembayaran (Auto Format Rp)
            OutlinedTextField(
                value = formatRupiah(sisaLong),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text(stringResource(R.string.label_remaining_payment)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(disabledBorderColor = DarkSurfaceVariant, disabledTextColor = if (sisaLong == 0L) Color(0xFF2E7D32) else AccentGold, disabledLabelColor = TextSecondary)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Catatan / Jaminan
            OutlinedTextField(
                value = catatan,
                onValueChange = { catatan = it },
                label = { Text(stringResource(R.string.label_notes_guarantee)) },
                placeholder = { Text("KTP / Ukuran M / DP Lunas") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Lanjut ke Pembayaran dengan Validasi Stok & Validasi Uang Muka / DP
            Button(
                onClick = {
                    val jmlInt = jumlahText.toIntOrNull() ?: 1
                    val dpVal = dpText.toLongOrNull() ?: 0L
                    val totalVal = hargaTotalText.toLongOrNull() ?: 0L

                    if (!isEditMode) {
                        if (selectedItem == null) {
                            viewModel.setErrorMessage("Silakan pilih barang terlebih dahulu.")
                            onChangeItemRequested()
                            return@Button
                        }
                        if (namaPenyewa.trim().isEmpty() || noWa.trim().isEmpty()) {
                            viewModel.setErrorMessage("Nama dan Nomor WA penyewa wajib diisi.")
                            return@Button
                        }
                        if (jmlInt <= 0) {
                            viewModel.setErrorMessage("Jumlah disewa minimal 1.")
                            return@Button
                        }
                        if (jmlInt > selectedItem.stokTersedia) {
                            viewModel.setErrorMessage("Jumlah disewa ($jmlInt) melebihi stok yang tersedia (${selectedItem.stokTersedia}).")
                            return@Button
                        }
                        if (dpVal > totalVal) {
                            viewModel.setErrorMessage("Uang Muka / DP tidak boleh melebihi Total Biaya Sewa.")
                            return@Button
                        }
                    } else {
                        if (namaPenyewa.trim().isEmpty() || noWa.trim().isEmpty()) {
                            viewModel.setErrorMessage("Nama dan Nomor WA penyewa wajib diisi.")
                            return@Button
                        }
                        if (dpVal > totalVal) {
                            viewModel.setErrorMessage("Uang Muka / DP tidak boleh melebihi Total Biaya Sewa.")
                            return@Button
                        }
                    }

                    showPaymentScreen = true
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkBackground, disabledContainerColor = DarkSurfaceVariant, disabledContentColor = TextSecondary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DarkBackground, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = stringResource(R.string.btn_pay),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
