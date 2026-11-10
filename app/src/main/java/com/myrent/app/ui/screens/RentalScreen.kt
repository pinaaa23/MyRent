package com.myrent.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.myrent.app.ui.components.AppBackground
import com.myrent.app.ui.katalog.CatalogViewModel
import com.myrent.app.ui.sewa.DetailSewaBottomSheet
import com.myrent.app.ui.sewa.PilihBarangScreen
import com.myrent.app.ui.sewa.RentalViewModel
import com.myrent.app.ui.sewa.SewaBaruBottomSheet
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.GoldBorderColor
import com.myrent.app.ui.theme.OnAccentGold
import com.myrent.app.ui.theme.StatusRed
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Layar Utama Sewa Barang / Transaksi (Responsif & Adaptif HP/Tablet)
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

    AppBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        rentalToEdit = null
                        selectedItemForSewa = null
                        rentalViewModel.resetUiState()
                        showPilihBarangScreen = true
                    },
                    containerColor = AccentGold,
                    contentColor = OnAccentGold,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Sewa Baru",
                            tint = OnAccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_new_rental),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = OnAccentGold
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1000.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.title_rentals),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { rentalViewModel.onSearchQueryChanged(it) },
                        placeholder = { Text(stringResource(R.string.placeholder_search_rental), color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cari", tint = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                                    selectedLabelColor = OnAccentGold,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                        // Grid Adaptif untuk Transaksi Sewa (1 kolom di HP, 2-3 kolom di Tablet)
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 340.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
        }
    }

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
    val todayMs = System.currentTimeMillis()
    val calToday = Calendar.getInstance().apply {
        timeInMillis = todayMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayStartMs = calToday.timeInMillis

    val isOverdue = rental.status == "berjalan" && rental.tanggalKembali < todayStartMs

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GoldBorderColor),
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

                val (badgeBg, badgeText) = when {
                    rental.status == "selesai" -> Color(0xFF2E7D32) to stringResource(R.string.filter_completed)
                    isOverdue -> StatusRed to "Terlambat"
                    rental.status == "terlambat" -> StatusRed to stringResource(R.string.filter_overdue)
                    else -> AccentGold to stringResource(R.string.filter_active)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rental.status == "berjalan" && !isOverdue) OnAccentGold else Color.White
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
                    color = if (isOverdue) StatusRed else TextSecondary
                )

                val paymentText = if (rental.sisaBayar == 0L) {
                    stringResource(R.string.status_paid)
                } else {
                    "SISA ${formatRupiah(rental.sisaBayar)}"
                }

                val paymentColor = if (rental.sisaBayar == 0L) Color(0xFF6BCB77) else StatusRed

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

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}
