package com.myrent.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myrent.app.R
import com.myrent.app.data.rentals.Rental
import com.myrent.app.ui.report.PeriodType
import com.myrent.app.ui.report.ReportBarChart
import com.myrent.app.ui.report.ReportViewModel
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Layar Laporan Omzet, Statistik Sewa, Grafik Batang, & Kategori
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    reportViewModel: ReportViewModel = viewModel()
) {
    val uiState by reportViewModel.uiState.collectAsState()

    var isCabangDropdownExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentGold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Memuat data laporan...", color = TextSecondary, fontSize = 14.sp)
                }
            }
            return@Scaffold
        }

        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = Color(0xFFFF6B6B),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { reportViewModel.loadReportData() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkBackground)
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Coba lagi")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Coba lagi", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp)
        ) {
            // 1. Header Judul & Dropdown Cabang
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.nav_report),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Dropdown Cabang
                Box {
                    val selectedCabangNama = remember(uiState.selectedCabangId, uiState.cabangList) {
                        if (uiState.selectedCabangId.isEmpty()) "Semua cabang"
                        else uiState.cabangList.find { it.id == uiState.selectedCabangId }?.nama ?: "Semua cabang"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { isCabangDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedCabangNama,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentGold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "Pilih Cabang", tint = AccentGold)
                        }
                    }

                    DropdownMenu(
                        expanded = isCabangDropdownExpanded,
                        onDismissRequest = { isCabangDropdownExpanded = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Semua cabang", color = TextPrimary) },
                            onClick = {
                                isCabangDropdownExpanded = false
                                reportViewModel.selectCabang("")
                            }
                        )
                        uiState.cabangList.forEach { cabang ->
                            DropdownMenuItem(
                                text = { Text(cabang.nama, color = TextPrimary) },
                                onClick = {
                                    isCabangDropdownExpanded = false
                                    reportViewModel.selectCabang(cabang.id)
                                }
                            )
                        }
                    }
                }
            }

            // 2. Chip Pilihan Periode (Hari, Minggu, Bulan, Tahun)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val periodOptions = listOf(
                    PeriodType.HARI to "Hari",
                    PeriodType.MINGGU to "Minggu",
                    PeriodType.BULAN to "Bulan",
                    PeriodType.TAHUN to "Tahun"
                )

                items(periodOptions) { (pType, pLabel) ->
                    val isSelected = uiState.selectedPeriodType == pType
                    FilterChip(
                        selected = isSelected,
                        onClick = { reportViewModel.selectPeriodType(pType) },
                        label = { Text(pLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Baris Navigasi Periode (Panah Kiri / Kanan & Teks Periode)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { reportViewModel.navigatePeriodOffset(-1) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sebelumnya", tint = TextPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.periodRange?.displayText ?: "",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (uiState.periodRange != null && !uiState.periodRange!!.isCurrentPeriod) {
                            Text(
                                text = uiState.periodRange!!.currentButtonText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                modifier = Modifier
                                    .clickable { reportViewModel.resetToCurrentPeriod() }
                                    .padding(top = 2.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { reportViewModel.navigatePeriodOffset(1) },
                        enabled = uiState.periodRange != null && !uiState.periodRange!!.isCurrentPeriod
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Berikutnya",
                            tint = if (uiState.periodRange != null && !uiState.periodRange!!.isCurrentPeriod) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Kartu Utama: Omzet Kotor & 3 Kartu Ringkasan
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Omzet Kotor", fontSize = 14.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatRupiah(uiState.omzetKotor),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total biaya sewa pada periode ini",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 Kartu Ringkasan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Transaksi
                        StatSubCard(
                            label = "Transaksi",
                            value = "${uiState.totalTransaksi}",
                            valueColor = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        // Sudah Diterima
                        StatSubCard(
                            label = "Sudah Diterima",
                            value = formatRupiah(uiState.sudahDiterima),
                            valueColor = Color(0xFF6BCB77),
                            modifier = Modifier.weight(1.2f)
                        )

                        // Belum Dibayar
                        StatSubCard(
                            label = "Belum Dibayar",
                            value = formatRupiah(uiState.belumDibayar),
                            valueColor = Color(0xFFFF6B6B),
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Grafik Batang Canvas
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ReportBarChart(
                    bars = uiState.chartBars,
                    selectedIndex = uiState.selectedBarIndex,
                    onBarSelected = { reportViewModel.selectBar(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Section "Barang Paling Sering Disewa"
            SectionTitle("Barang Paling Sering Disewa (Top 5)")

            if (uiState.topItems.isEmpty()) {
                EmptySectionMessage("Belum ada transaksi pada periode ini.")
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        uiState.topItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}. ${item.namaBarang}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${item.jumlahSewaCount}x disewa",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 7. Section "Omzet per Kategori"
            SectionTitle("Omzet per Kategori")

            if (uiState.categoryStats.isEmpty()) {
                EmptySectionMessage("Belum ada transaksi pada periode ini.")
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        uiState.categoryStats.forEach { cat ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat.namaKategori, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(formatRupiah(cat.totalOmzet), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { cat.proportion },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AccentGold,
                                    trackColor = DarkSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8. Section "Daftar Transaksi"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Transaksi (${uiState.allRentalsInPeriod.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (uiState.allRentalsInPeriod.size > 10) {
                    TextButton(onClick = { reportViewModel.setShowAllRentalsDialog(true) }) {
                        Text("Lihat semua", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.recentRentals.isEmpty()) {
                EmptySectionMessage("Belum ada transaksi pada periode ini.")
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.recentRentals.forEach { rental ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rental.namaBarang, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                    Text("${rental.namaPenyewa} • ${formatDate(rental.dibuatPada)}", color = TextSecondary, fontSize = 12.sp)
                                }
                                Text(formatRupiah(rental.hargaTotal), fontWeight = FontWeight.Bold, color = AccentGold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Lihat Semua Transaksi
    if (uiState.showAllRentalsDialog) {
        AllRentalsDialog(
            rentals = uiState.allRentalsInPeriod,
            onDismiss = { reportViewModel.setShowAllRentalsDialog(false) }
        )
    }
}

@Composable
fun StatSubCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun EmptySectionMessage(msg: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(msg, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Dialog Menampilkan Semua Transaksi dalam Periode
 */
@Composable
fun AllRentalsDialog(
    rentals: List<Rental>,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Semua Transaksi (${rentals.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(onClick = onDismiss) {
                    Text("Tutup", color = AccentGold, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rentals.forEach { rental ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(rental.namaBarang, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                Text(formatRupiah(rental.hargaTotal), fontWeight = FontWeight.Bold, color = AccentGold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${rental.namaPenyewa} (${rental.noWa})", color = TextSecondary, fontSize = 12.sp)
                            Text("${formatDate(rental.dibuatPada)} • ${rental.lamaHari} Hari", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
