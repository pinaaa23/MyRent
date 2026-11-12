package com.myrent.app.ui.sewa

import android.app.DatePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.myrent.app.data.rentals.RentalItem
import com.myrent.app.ui.screens.formatDate
import com.myrent.app.ui.screens.formatRupiah
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.GoldBorderColor
import com.myrent.app.ui.theme.OnAccentGold
import com.myrent.app.ui.theme.StatusRed
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary
import java.util.Calendar
import java.util.concurrent.TimeUnit

fun calculateDays(startMs: Long, endMs: Long): Int {
    val diff = endMs - startMs
    val days = TimeUnit.MILLISECONDS.toDays(diff).toInt()
    return if (days <= 0) 1 else days
}

data class RentalItemRowState(
    val item: Item,
    var jumlah: Int = 1
)

/**
 * Bottom Sheet Formulir "Sewa Baru" dengan Preservasi Form Isian & Media Sosial Konsumen Wajib
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SewaBaruBottomSheet(
    sheetState: SheetState,
    viewModel: RentalViewModel,
    selectedItems: List<Item> = emptyList(),
    selectedItem: Item? = selectedItems.firstOrNull(),
    rentalToEdit: Rental? = null,
    fotoRepository: FotoRepository = FotoRepository(),
    onChangeItemRequested: (List<Item>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = rentalToEdit != null
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is RentalUiState.Loading

    val customerList by viewModel.customerList.collectAsState()
    val draftForm by viewModel.draftForm.collectAsState()

    val userDoc by viewModel.userDocState.collectAsState()
    val kasirNama = userDoc.nama.ifEmpty { "Admin MyRent" }

    // Membaca dari draftForm jika sedang menambah barang agar isian form tidak hilang
    var namaPenyewa by remember(rentalToEdit) { mutableStateOf(rentalToEdit?.namaPenyewa ?: draftForm.namaPenyewa) }
    var noWa by remember(rentalToEdit) { mutableStateOf(rentalToEdit?.noWa ?: draftForm.noWa) }
    var medsosPenyewa by remember(rentalToEdit) { mutableStateOf(rentalToEdit?.medsosPenyewa ?: draftForm.medsosPenyewa) }

    var showNamaSuggestions by remember { mutableStateOf(false) }
    var showWaSuggestions by remember { mutableStateOf(false) }

    val initialStartMs = rentalToEdit?.tanggalMulai ?: if (draftForm.tglMulaiMs > 0L) draftForm.tglMulaiMs else System.currentTimeMillis()
    val initialEndMs = rentalToEdit?.tanggalKembali ?: if (draftForm.tglKembaliMs > 0L) draftForm.tglKembaliMs else (System.currentTimeMillis() + 86400000L)

    val calStart = remember(rentalToEdit) { Calendar.getInstance().apply { timeInMillis = initialStartMs } }
    val calEnd = remember(rentalToEdit) { Calendar.getInstance().apply { timeInMillis = initialEndMs } }

    var tglMulaiMs by remember(rentalToEdit) { mutableStateOf(calStart.timeInMillis) }
    var tglKembaliMs by remember(rentalToEdit) { mutableStateOf(calEnd.timeInMillis) }

    var lamaHari by remember(tglMulaiMs, tglKembaliMs) { mutableStateOf(calculateDays(tglMulaiMs, tglKembaliMs)) }

    // Inisialisasi daftar barang yang disewa
    val rentedItemsList = remember(rentalToEdit) {
        val list = mutableStateListOf<RentalItemRowState>()
        if (rentalToEdit != null) {
            rentalToEdit.getItemsList().forEach { rItem ->
                list.add(
                    RentalItemRowState(
                        item = Item(
                            id = rItem.itemId,
                            nama = rItem.nama,
                            kategori = rItem.kategori,
                            hargaSewa = rItem.hargaSatuan,
                            stokTersedia = rItem.jumlah.coerceAtLeast(10),
                            fotoBase64 = rItem.fotoBase64
                        ),
                        jumlah = rItem.jumlah
                    )
                )
            }
        } else if (selectedItems.isNotEmpty()) {
            selectedItems.forEach { item ->
                list.add(RentalItemRowState(item = item, jumlah = 1))
            }
        } else if (selectedItem != null) {
            list.add(RentalItemRowState(item = selectedItem, jumlah = 1))
        }
        list
    }

    // Sinkronisasi barang terpilih saat pengguna menambah/mengubah barang dari Katalog tanpa menghapus isian form
    LaunchedEffect(selectedItems) {
        if (rentalToEdit == null && selectedItems.isNotEmpty()) {
            val existingQtyMap = rentedItemsList.associate { it.item.id to it.jumlah }
            rentedItemsList.clear()
            selectedItems.forEach { item ->
                val qty = existingQtyMap[item.id] ?: 1
                rentedItemsList.add(RentalItemRowState(item = item, jumlah = qty))
            }
        }
    }

    // Hitung otomatis total biaya dari sum(hargaSatuan * jumlah) * lamaHari
    val calculatedTotal = remember(rentedItemsList.map { it.jumlah }, rentedItemsList.map { it.item.id }, lamaHari) {
        val sumUnit = rentedItemsList.sumOf { it.item.hargaSewa * it.jumlah.toLong() }
        sumUnit * lamaHari.toLong()
    }

    var hargaTotalText by remember(calculatedTotal, rentalToEdit) {
        mutableStateOf(if (rentalToEdit != null) rentalToEdit.hargaTotal.toString() else calculatedTotal.toString())
    }

    LaunchedEffect(calculatedTotal) {
        if (!isEditMode) {
            hargaTotalText = calculatedTotal.toString()
        }
    }

    // dpText default kosong (tanpa prefilled "0" yang harus dihapus)
    var dpText by remember(rentalToEdit) {
        mutableStateOf(if (rentalToEdit != null && rentalToEdit.dp > 0L) rentalToEdit.dp.toString() else draftForm.dpText)
    }

    var catatan by remember(rentalToEdit) { mutableStateOf(rentalToEdit?.catatan ?: draftForm.catatan) }

    var showPaymentScreen by remember(rentalToEdit) { mutableStateOf(false) }
    var createdRentalForNota by remember(rentalToEdit) { mutableStateOf<Rental?>(null) }

    LaunchedEffect(Unit) {
        showPaymentScreen = false
        createdRentalForNota = null
    }

    // Fungsi simpan draft otomatis ke ViewModel
    fun saveFormToDraft() {
        viewModel.updateDraftForm(
            namaPenyewa = namaPenyewa,
            noWa = noWa,
            medsosPenyewa = medsosPenyewa,
            tglMulaiMs = tglMulaiMs,
            tglKembaliMs = tglKembaliMs,
            dpText = dpText,
            catatan = catatan,
            selectedItems = rentedItemsList.map { it.item }
        )
    }

    val totalLong = hargaTotalText.toLongOrNull() ?: 0L
    val dpLong = dpText.toLongOrNull() ?: 0L
    val sisaLong = if (totalLong > dpLong) totalLong - dpLong else 0L

    val scrollState = rememberScrollState()

    val nameSuggestions = remember(namaPenyewa, customerList, showNamaSuggestions) {
        if (showNamaSuggestions && namaPenyewa.trim().isNotEmpty()) {
            customerList
                .filter { it.nama.contains(namaPenyewa, ignoreCase = true) }
                .distinctBy { it.nama.trim().lowercase() }
                .take(5)
        } else emptyList()
    }

    val waSuggestions = remember(noWa, customerList, showWaSuggestions) {
        if (showWaSuggestions && noWa.trim().isNotEmpty()) {
            val normInput = normalizeNoWa(noWa)
            customerList
                .filter { normalizeNoWa(it.noWa).contains(normInput) || it.noWa.contains(noWa) }
                .distinctBy { normalizeNoWa(it.noWa) }
                .take(5)
        } else emptyList()
    }

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
                viewModel.clearDraftForm()
                onDismiss()
            }
        )
        return
    }

    if (showPaymentScreen) {
        val primaryName = rentedItemsList.firstOrNull()?.item?.nama ?: "Barang Sewa"
        val displayName = if (rentedItemsList.size > 1) "$primaryName +${rentedItemsList.size - 1} lainnya" else primaryName
        val totalUnits = rentedItemsList.sumOf { it.jumlah }

        PembayaranScreen(
            totalBayar = dpLong,
            namaPenyewa = namaPenyewa,
            namaBarang = displayName,
            jumlahUnit = totalUnits,
            cabangNama = userDoc.namaToko.ifEmpty { "MYRENT STORE" },
            onBack = { showPaymentScreen = false },
            onKonfirmasiPembayaran = { metode ->
                val rentalItemsToSave = rentedItemsList.map {
                    RentalItem(
                        itemId = it.item.id,
                        nama = it.item.nama,
                        kategori = it.item.kategori,
                        hargaSatuan = it.item.hargaSewa,
                        jumlah = it.jumlah,
                        fotoBase64 = it.item.fotoBase64
                    )
                }

                if (isEditMode) {
                    viewModel.updateSewa(
                        rental = rentalToEdit!!.copy(items = rentalItemsToSave),
                        namaPenyewa = namaPenyewa,
                        noWa = noWa,
                        medsosPenyewa = medsosPenyewa,
                        tanggalMulai = tglMulaiMs,
                        tanggalKembali = tglKembaliMs,
                        lamaHari = lamaHari,
                        jumlah = totalUnits,
                        hargaTotal = totalLong,
                        dp = dpLong,
                        metodePembayaran = metode,
                        catatan = catatan,
                        onSuccess = {
                            showPaymentScreen = false
                            viewModel.clearDraftForm()
                            onDismiss()
                        }
                    )
                } else {
                    val primaryItem = rentedItemsList.firstOrNull()?.item

                    viewModel.prosesPembayaranDanSimpanSewa(
                        selectedItem = primaryItem,
                        namaPenyewa = namaPenyewa,
                        noWa = noWa,
                        medsosPenyewa = medsosPenyewa,
                        tanggalMulai = tglMulaiMs,
                        tanggalKembali = tglKembaliMs,
                        lamaHari = lamaHari,
                        jumlah = totalUnits,
                        hargaTotal = totalLong,
                        dp = dpLong,
                        metodePembayaran = metode,
                        catatan = catatan,
                        rentedItems = rentalItemsToSave,
                        onSuccess = { savedRental ->
                            showPaymentScreen = false
                            viewModel.clearDraftForm()
                            createdRentalForNota = savedRental
                        }
                    )
                }
            }
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.clearDraftForm()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (isEditMode) stringResource(R.string.btn_edit_rental) else stringResource(R.string.title_add_rental),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState is RentalUiState.Error) {
                Text(
                    text = (uiState as RentalUiState.Error).message,
                    color = StatusRed,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            // BAGIAN "Barang yang disewa"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Barang yang disewa (${rentedItemsList.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (!isEditMode) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                saveFormToDraft()
                                onChangeItemRequested(rentedItemsList.map { it.item })
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("+ Tambah Barang", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (rentedItemsList.isEmpty()) {
                Text("Belum ada barang dipilih", fontSize = 13.sp, color = StatusRed)
            } else {
                rentedItemsList.forEachIndexed { index, rowState ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldBorderColor),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bitmap = remember(rowState.item.fotoBase64) {
                                if (rowState.item.fotoBase64.isNotEmpty()) fotoRepository.base64ToBitmap(rowState.item.fotoBase64) else null
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (bitmap != null) {
                                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = rowState.item.nama, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Filled.Checkroom, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(rowState.item.nama, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp, maxLines = 1)
                                Text("${formatRupiah(rowState.item.hargaSewa)} / unit", fontSize = 11.sp, color = TextSecondary)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (rowState.jumlah > 1) {
                                            rentedItemsList[index] = rowState.copy(jumlah = rowState.jumlah - 1)
                                        }
                                    },
                                    enabled = rowState.jumlah > 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Kurangi", tint = if (rowState.jumlah > 1) AccentGold else TextSecondary)
                                }

                                Text("${rowState.jumlah}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 6.dp))

                                IconButton(
                                    onClick = {
                                        if (rowState.jumlah < rowState.item.stokTersedia) {
                                            rentedItemsList[index] = rowState.copy(jumlah = rowState.jumlah + 1)
                                        }
                                    },
                                    enabled = rowState.jumlah < rowState.item.stokTersedia,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.AddCircleOutline, contentDescription = "Tambah", tint = if (rowState.jumlah < rowState.item.stokTersedia) AccentGold else TextSecondary)
                                }

                                if (rentedItemsList.size > 1 && !isEditMode) {
                                    IconButton(
                                        onClick = { rentedItemsList.removeAt(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = StatusRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nama Penyewa
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = namaPenyewa,
                    onValueChange = { input ->
                        namaPenyewa = input
                        saveFormToDraft()
                        showNamaSuggestions = true
                        viewModel.resetUiState()
                    },
                    label = { Text(stringResource(R.string.label_tenant_name)) },
                    placeholder = { Text("misal: Budi Santoso") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )

                if (nameSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column {
                            nameSuggestions.forEach { customer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            namaPenyewa = customer.nama
                                            noWa = customer.noWa
                                            saveFormToDraft()
                                            showNamaSuggestions = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "${customer.nama} (${customer.noWa})", color = TextPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nomor WA Penyewa
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = noWa,
                    onValueChange = { input ->
                        noWa = input.filter { it.isDigit() || it == '+' }
                        saveFormToDraft()
                        showWaSuggestions = true
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

                if (waSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column {
                            waSuggestions.forEach { customer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            namaPenyewa = customer.nama
                                            noWa = customer.noWa
                                            saveFormToDraft()
                                            showWaSuggestions = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "${customer.nama} (${customer.noWa})", color = TextPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Media Sosial Konsumen (IG / TikTok / FB) (Wajib)
            OutlinedTextField(
                value = medsosPenyewa,
                onValueChange = { input ->
                    medsosPenyewa = input
                    saveFormToDraft()
                    viewModel.resetUiState()
                },
                label = { Text("Media Sosial Konsumen (IG / TikTok / FB) (Wajib)") },
                placeholder = { Text("misal: @budi_santoso") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pemilih Tanggal
            Row(modifier = Modifier.fillMaxWidth()) {
                val startDatePicker = DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val cal = Calendar.getInstance().apply { set(y, m, d) }
                        tglMulaiMs = cal.timeInMillis
                        if (tglKembaliMs <= tglMulaiMs) {
                            tglKembaliMs = tglMulaiMs + 86400000L
                        }
                        lamaHari = calculateDays(tglMulaiMs, tglKembaliMs)
                        saveFormToDraft()
                    },
                    calStart.get(Calendar.YEAR), calStart.get(Calendar.MONTH), calStart.get(Calendar.DAY_OF_MONTH)
                )

                val endDatePicker = DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val cal = Calendar.getInstance().apply { set(y, m, d) }
                        val endMs = cal.timeInMillis
                        if (endMs > tglMulaiMs) {
                            tglKembaliMs = endMs
                            lamaHari = calculateDays(tglMulaiMs, tglKembaliMs)
                            saveFormToDraft()
                        }
                    },
                    calEnd.get(Calendar.YEAR), calEnd.get(Calendar.MONTH), calEnd.get(Calendar.DAY_OF_MONTH)
                )

                OutlinedTextField(
                    value = formatDate(tglMulaiMs),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_start_date)) },
                    trailingIcon = {
                        IconButton(onClick = { startDatePicker.show() }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Tgl Mulai", tint = AccentGold)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                        .clickable { startDatePicker.show() },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = formatDate(tglKembaliMs),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_end_date)) },
                    trailingIcon = {
                        IconButton(onClick = { endDatePicker.show() }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Tgl Kembali", tint = AccentGold)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                        .clickable { endDatePicker.show() },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Durasi Sewa
            OutlinedTextField(
                value = "$lamaHari Hari",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text(stringResource(R.string.label_duration)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = DarkSurfaceVariant,
                    disabledLabelColor = AccentGold,
                    disabledTextColor = AccentGold
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Total Biaya Sewa
            OutlinedTextField(
                value = hargaTotalText,
                onValueChange = { input ->
                    hargaTotalText = input.filter { it.isDigit() }
                    viewModel.resetUiState()
                },
                label = { Text(stringResource(R.string.label_total_price)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Input Uang Muka / DP (placeholder "0", default kosong)
            OutlinedTextField(
                value = dpText,
                onValueChange = { input ->
                    dpText = input.filter { it.isDigit() }
                    saveFormToDraft()
                    viewModel.resetUiState()
                },
                label = { Text(stringResource(R.string.label_dp)) },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sisa Pembayaran
            OutlinedTextField(
                value = formatRupiah(sisaLong),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text(stringResource(R.string.label_remaining_payment)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = DarkSurfaceVariant,
                    disabledLabelColor = if (sisaLong == 0L) Color(0xFF6BCB77) else StatusRed,
                    disabledTextColor = if (sisaLong == 0L) Color(0xFF6BCB77) else StatusRed
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Catatan / Jaminan Teks
            OutlinedTextField(
                value = catatan,
                onValueChange = { input ->
                    catatan = input
                    saveFormToDraft()
                },
                label = { Text("Catatan / Jaminan (Ukuran/KTP/dll)") },
                placeholder = { Text("misal: KTP / SIM / Kartu Pelajar") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Lanjut ke Pembayaran / Simpan
            Button(
                onClick = {
                    val dpVal = dpText.toLongOrNull() ?: 0L
                    val totalVal = hargaTotalText.toLongOrNull() ?: 0L

                    if (rentedItemsList.isEmpty()) {
                        viewModel.setErrorMessage("Daftar barang yang disewa tidak boleh kosong.")
                        return@Button
                    }
                    if (namaPenyewa.trim().isEmpty() || noWa.trim().isEmpty()) {
                        viewModel.setErrorMessage("Nama dan Nomor WA penyewa wajib diisi.")
                        return@Button
                    }
                    if (medsosPenyewa.trim().isEmpty()) {
                        viewModel.setErrorMessage("Media sosial konsumen (IG/TikTok/FB) wajib diisi.")
                        return@Button
                    }
                    if (dpVal < 0L) {
                        viewModel.setErrorMessage("Uang muka / DP tidak boleh negatif.")
                        return@Button
                    }
                    if (dpVal > totalVal) {
                        viewModel.setErrorMessage("Uang Muka / DP tidak boleh melebihi Total Biaya Sewa.")
                        return@Button
                    }

                    // Cek stok seluruh barang
                    for (rState in rentedItemsList) {
                        if (rState.jumlah > rState.item.stokTersedia && !isEditMode) {
                            viewModel.setErrorMessage("Jumlah barang \"${rState.item.nama}\" (${rState.jumlah}) melebihi stok yang tersedia (${rState.item.stokTersedia}).")
                            return@Button
                        }
                    }

                    val rentalItemsToSave = rentedItemsList.map {
                        RentalItem(
                            itemId = it.item.id,
                            nama = it.item.nama,
                            kategori = it.item.kategori,
                            hargaSatuan = it.item.hargaSewa,
                            jumlah = it.jumlah,
                            fotoBase64 = it.item.fotoBase64
                        )
                    }

                    // Jika DP = 0: langsung simpan tanpa masuk ke layar pembayaran/QRIS
                    if (dpVal == 0L && !isEditMode) {
                        val primaryItem = rentedItemsList.firstOrNull()?.item
                        val totalUnits = rentedItemsList.sumOf { it.jumlah }

                        viewModel.prosesPembayaranDanSimpanSewa(
                            selectedItem = primaryItem,
                            namaPenyewa = namaPenyewa,
                            noWa = noWa,
                            medsosPenyewa = medsosPenyewa,
                            tanggalMulai = tglMulaiMs,
                            tanggalKembali = tglKembaliMs,
                            lamaHari = lamaHari,
                            jumlah = totalUnits,
                            hargaTotal = totalVal,
                            dp = 0L,
                            metodePembayaran = "-",
                            catatan = catatan,
                            rentedItems = rentalItemsToSave,
                            onSuccess = { savedRental ->
                                viewModel.clearDraftForm()
                                createdRentalForNota = savedRental
                            }
                        )
                        return@Button
                    }

                    showPaymentScreen = true
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold, contentColor = OnAccentGold,
                    disabledContainerColor = DarkSurfaceVariant, disabledContentColor = TextSecondary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OnAccentGold, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (dpLong > 0L) stringResource(R.string.btn_pay) else stringResource(R.string.btn_save),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnAccentGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
