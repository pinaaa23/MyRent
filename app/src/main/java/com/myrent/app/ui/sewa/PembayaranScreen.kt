package com.myrent.app.ui.sewa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.R
import com.myrent.app.ui.components.AppBackground
import com.myrent.app.ui.screens.formatRupiah
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.GoldBorderColor
import com.myrent.app.ui.theme.OnAccentGold
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Layar Full-Screen Pembayaran (Pilihan Tunai vs Tampilan Full QRIS dengan Nominal DP / Pelunasan yang Presisi)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PembayaranScreen(
    totalBayar: Long, // Nominal yang dibayar sekarang (DP saat sewa baru, Sisa Bayar saat pelunasan)
    namaPenyewa: String,
    namaBarang: String,
    jumlahUnit: Int,
    cabangNama: String,
    onBack: () -> Unit,
    onKonfirmasiPembayaran: (metodePembayaran: String) -> Unit
) {
    var selectedMetode by remember { mutableStateOf("QRIS") }
    var isQrisFullScreen by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // 1. Tampilan Full-Screen QRIS jika pengguna memilih QRIS Penuh
    if (isQrisFullScreen) {
        FullScreenQrisView(
            totalBayar = totalBayar,
            namaPenyewa = namaPenyewa,
            namaBarang = namaBarang,
            jumlahUnit = jumlahUnit,
            cabangNama = cabangNama,
            onBack = { isQrisFullScreen = false },
            onConfirm = { onKonfirmasiPembayaran("QRIS") }
        )
        return
    }

    // 2. Layar Utama Pembayaran
    AppBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_payment_method), color = TextPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
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
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Kartu Ringkasan Biaya Dibayar Sekarang (Nominal DP)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GoldBorderColor),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nominal DP yang Harus Dibayar",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRupiah(totalBayar),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "$namaBarang ($jumlahUnit unit) • $namaPenyewa",
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Pilih Metode Pembayaran",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Opsi 1: QRIS
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMetode = "QRIS" },
                    shape = RoundedCornerShape(16.dp),
                    border = if (selectedMetode == "QRIS") BorderStroke(2.dp, AccentGold) else BorderStroke(1.dp, GoldBorderColor),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMetode == "QRIS") DarkSurfaceVariant else DarkSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMetode == "QRIS",
                            onClick = { selectedMetode = "QRIS" },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentGold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = "QRIS",
                            tint = AccentGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "QRIS",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Scan via BCA, GoPay, OVO, Dana, ShopeePay",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Opsi 2: Tunai (Cash)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMetode = "Tunai" },
                    shape = RoundedCornerShape(16.dp),
                    border = if (selectedMetode == "Tunai") BorderStroke(2.dp, AccentGold) else BorderStroke(1.dp, GoldBorderColor),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMetode == "Tunai") DarkSurfaceVariant else DarkSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMetode == "Tunai",
                            onClick = { selectedMetode = "Tunai" },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentGold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Money,
                            contentDescription = "Tunai",
                            tint = AccentGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tunai (Cash)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Pembayaran tunai langsung di kasir",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Aksi
                if (selectedMetode == "QRIS") {
                    Button(
                        onClick = { isQrisFullScreen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = OnAccentGold)
                    ) {
                        Icon(imageVector = Icons.Filled.QrCode2, contentDescription = "QRIS", tint = OnAccentGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tampilkan Kode QRIS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnAccentGold)
                    }
                } else {
                    Button(
                        onClick = { onKonfirmasiPembayaran("Tunai") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = OnAccentGold)
                    ) {
                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Konfirmasi", tint = OnAccentGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Konfirmasi Pembayaran Tunai", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnAccentGold)
                    }
                }
            }
        }
    }
}

/**
 * Tampilan Khusus Full-Screen Kode QRIS (Layar Penuh HP)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenQrisView(
    totalBayar: Long,
    namaPenyewa: String,
    namaBarang: String,
    jumlahUnit: Int,
    cabangNama: String,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    AppBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pembayaran QRIS", color = TextPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = cabangNama.ifEmpty { "MYRENT STORE" }.uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatRupiah(totalBayar),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$namaBarang ($jumlahUnit unit) • $namaPenyewa",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                // Kartu Putih Kode QRIS Penuh Presisi
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "QRIS REGIONAL",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = "QR Code Full",
                            tint = Color.Black,
                            modifier = Modifier.size(240.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "NMB: ${cabangNama.ifEmpty { "MYRENT STORE" }}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Poin Penjualan Alat Rent & Sewa",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Silakan tunjukkan kode QRIS di atas kepada konsumen untuk dipindai.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = OnAccentGold)
                    ) {
                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Konfirmasi", tint = OnAccentGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Konfirmasi Pembayaran Selesai", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnAccentGold)
                    }
                }
            }
        }
    }
}
