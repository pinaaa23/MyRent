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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.myrent.app.data.items.Item
import com.myrent.app.data.rentals.Rental
import com.myrent.app.ui.katalog.CatalogViewModel
import com.myrent.app.ui.sewa.DetailSewaBottomSheet
import com.myrent.app.ui.sewa.PilihBarangScreen
import com.myrent.app.ui.sewa.RentalViewModel
import com.myrent.app.ui.sewa.SewaBaruBottomSheet
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layar Utama Sewa Barang / Transaksi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalScreen(
    rentalViewModel: RentalViewModel = viewModel(),
    catalogViewModel: CatalogViewModel = viewModel()
) {
    val rentals by rentalViewModel.filteredRentals.collectAsState()
    val searchQuery by rentalViewModel.searchQuery.collectAsState()
    val selectedFilter by rentalViewModel.selectedFilter.collectAsState()

    var showPilihBarangScreen by remember { mutableStateOf(false) }
    var showSewaBaruBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForSewa by remember { mutableStateOf<Item?>(null) }

    var selectedRentalForDetail by remember { mutableStateOf<Rental?>(null) }
    var rentalToEdit by remember { mutableStateOf<Rental?>(null) }

    val sheetStateForm = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetStateDetail = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filterOptions = listOf("Semua", "Berjalan", "Hampir Jatuh Tempo", "Terlambat", "Selesai")

    // LANGKAH 1: Layar Penuh "Pilih Barang"
    if (showPilihBarangScreen) {
        PilihBarangScreen(
            catalogViewModel = catalogViewModel,
            initialSelectedItem = selectedItemForSewa,
            onBack = { showPilihBarangScreen = false },
            onBarangDipilih = { item ->
                selectedItemForSewa = item
                showPilihBarangScreen = false
                showSewaBaruBottomSheet = true
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    rentalToEdit = null
                    selectedItemForSewa = null
                    rentalViewModel.resetUiState()
                    showPilihBarangScreen = true
                },
                containerColor = AccentGold,
                contentColor = DarkBackground,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Sewa Baru")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_new_rental),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Judul
            Text(
                text = stringResource(id = R.string.title_rentals),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            )

            // Input Pencarian
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { rentalViewModel.onSearchQueryChanged(it) },
                placeholder = { Text(stringResource(R.string.placeholder_search_rental)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cari", tint = TextSecondary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Baris Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { rentalViewModel.onFilterSelected(filter) },
                        label = { Text(filter) },
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

            // List Transaksi Sewa / Tampilan Kosong
            if (rentals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = "Kosong",
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.empty_rentals_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.empty_rentals_desc),
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rentals, key = { it.id }) { rental ->
                        RentalCard(
                            rental = rental,
                            onClick = {
                                selectedRentalForDetail = rental
                            }
                        )
                    }
                }
            }
        }
    }

    // LANGKAH 2: Bottom Sheet Form Sewa Baru / Edit
    if (showSewaBaruBottomSheet) {
        SewaBaruBottomSheet(
            sheetState = sheetStateForm,
            viewModel = rentalViewModel,
            selectedItem = selectedItemForSewa,
            rentalToEdit = rentalToEdit,
            onChangeItemRequested = {
                showSewaBaruBottomSheet = false
                showPilihBarangScreen = true
            },
            onDismiss = { showSewaBaruBottomSheet = false }
        )
    }

    // Bottom Sheet Detail Sewa
    if (selectedRentalForDetail != null) {
        DetailSewaBottomSheet(
            sheetState = sheetStateDetail,
            rental = selectedRentalForDetail!!,
            viewModel = rentalViewModel,
            onEditRequested = {
                rentalToEdit = selectedRentalForDetail
                selectedRentalForDetail = null
                showSewaBaruBottomSheet = true
            },
            onDismiss = { selectedRentalForDetail = null }
        )
    }
}

/**
 * Kartu Transaksi Sewa di Daftar
 */
@Composable
fun RentalCard(
    rental: Rental,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rental.namaBarang,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Status Badge
                val (badgeBg, badgeText) = when (rental.status) {
                    "selesai" -> Color(0xFF2E7D32) to stringResource(R.string.filter_completed)
                    "terlambat" -> Color(0xFFC62828) to stringResource(R.string.filter_overdue)
                    else -> AccentGold to stringResource(R.string.filter_active)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rental.status == "berjalan") DarkBackground else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${rental.namaPenyewa} (${rental.noWa})",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatDate(rental.tanggalMulai)} - ${formatDate(rental.tanggalKembali)} (${rental.lamaHari} Hari)",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                val paymentText = if (rental.sisaBayar == 0L) {
                    stringResource(R.string.status_paid)
                } else {
                    "SISA ${formatRupiah(rental.sisaBayar)}"
                }

                val paymentColor = if (rental.sisaBayar == 0L) Color(0xFF6BCB77) else Color(0xFFFF6B6B)

                Text(
                    text = paymentText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = paymentColor
                )
            }
        }
    }
}

/**
 * Helper Format Timestamp ke String Tanggal (contoh: 01 Nov 2025)
 */
fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}
