package com.myrent.app.ui.sewa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.R
import com.myrent.app.data.rentals.Rental
import com.myrent.app.ui.screens.formatDate
import com.myrent.app.ui.screens.formatRupiah
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Bottom Sheet Detail Transaksi Sewa & Fitur Pelunasan Pembayaran (QRIS / Tunai)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSewaBottomSheet(
    sheetState: SheetState,
    rental: Rental,
    viewModel: RentalViewModel,
    onEditRequested: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val kasirNama by viewModel.kasirNama.collectAsState()
    val cabangNama by viewModel.cabangNama.collectAsState()
    val cabangAlamat by viewModel.cabangAlamat.collectAsState()
    val cabangNoWa by viewModel.cabangNoWa.collectAsState()

    var showReturnConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPelunasanScreen by remember { mutableStateOf(false) }
    var updatedRentalForNota by remember { mutableStateOf<Rental?>(null) }

    val scrollState = rememberScrollState()

    // 1. Tampilkan Nota Transaksi jika pelunasan telah diproses
    if (updatedRentalForNota != null) {
        NotaTransaksiDialog(
            rental = updatedRentalForNota!!,
            kasirNama = kasirNama,
            cabangNama = cabangNama,
            cabangAlamat = cabangAlamat,
            cabangNoWa = cabangNoWa,
            viewModel = viewModel,
            onClose = {
                updatedRentalForNota = null
                onDismiss()
            }
        )
        return
    }

    // 2. Tampilkan Layar Full-Screen Pembayaran Pelunasan (Tunai vs QRIS)
    if (showPelunasanScreen) {
        PembayaranScreen(
            totalBayar = rental.sisaBayar,
            namaPenyewa = rental.namaPenyewa,
            namaBarang = rental.namaBarang,
            jumlahUnit = rental.jumlah,
            cabangNama = cabangNama,
            onBack = { showPelunasanScreen = false },
            onKonfirmasiPembayaran = { metode ->
                showPelunasanScreen = false
                viewModel.lunasiPembayaran(rental, metode) { savedRental ->
                    updatedRentalForNota = savedRental
                }
            }
        )
        return
    }

    // 3. Bottom Sheet Detail Utama
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header Barang & Penyewa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rental.namaBarang,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${rental.namaPenyewa} (${rental.noWa})",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                // Badge Status
                val (badgeBg, badgeText) = when (rental.status) {
                    "selesai" -> Color(0xFF2E7D32) to stringResource(R.string.filter_completed)
                    "terlambat" -> Color(0xFFC62828) to stringResource(R.string.filter_overdue)
                    else -> AccentGold to stringResource(R.string.filter_active)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rental.status == "berjalan") DarkBackground else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card Detail Rincian Biaya
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailRow(label = "Periode Sewa", value = "${formatDate(rental.tanggalMulai)} - ${formatDate(rental.tanggalKembali)} (${rental.lamaHari} Hari)")
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(label = "Total Biaya", value = formatRupiah(rental.hargaTotal))
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(label = "Uang Muka (DP)", value = formatRupiah(rental.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(
                        label = "Sisa Pembayaran",
                        value = if (rental.sisaBayar == 0L) stringResource(R.string.status_paid) else formatRupiah(rental.sisaBayar),
                        valueColor = if (rental.sisaBayar == 0L) Color(0xFF6BCB77) else Color(0xFFFF6B6B)
                    )
                    if (rental.catatan.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(label = "Catatan / Jaminan", value = rental.catatan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // FITUR PELUNASAN PEMBAYARAN (Aktif jika sisaBayar > 0, Mati jika Lunas)
            if (rental.sisaBayar > 0L) {
                Button(
                    onClick = { showPelunasanScreen = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkBackground)
                ) {
                    Icon(imageVector = Icons.Filled.Payment, contentDescription = "Pelunasan", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pelunasan Pembayaran (${formatRupiah(rental.sisaBayar)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            } else {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = Color(0xFF6BCB77)
                    )
                ) {
                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Lunas", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pembayaran Lunas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tombol Kirim WA Nota
            Button(
                onClick = { viewModel.kirimWhatsAppNota(context, rental, cabangNama) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim WA Nota", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.btn_send_wa), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            // Tombol Sudah Dikembalikan (Jika status masih berjalan)
            if (rental.status == "berjalan") {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { showReturnConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextPrimary)
                ) {
                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Selesai", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.btn_mark_returned), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Baris Tombol Edit & Hapus
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onEditRequested()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold)
                ) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.btn_edit_rental), fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B))
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.btn_delete), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialog Konfirmasi Pengembalian Barang
    if (showReturnConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showReturnConfirmDialog = false },
            title = { Text(stringResource(R.string.confirm_return_title), color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.confirm_return_msg, rental.namaBarang), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showReturnConfirmDialog = false
                    viewModel.tandaiDikembalikan(rental, onSuccess = onDismiss)
                }) {
                    Text("Ya, Dikembalikan", color = AccentGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnConfirmDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Dialog Konfirmasi Hapus Transaksi Sewa
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Sewa?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus catatan sewa ini?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    viewModel.hapusSewa(rental, onSuccess = onDismiss)
                }) {
                    Text(stringResource(R.string.btn_delete), color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
