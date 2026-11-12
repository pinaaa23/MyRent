package com.myrent.app.ui.katalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.R
import com.myrent.app.data.foto.FotoRepository
import com.myrent.app.data.items.Item
import com.myrent.app.data.kategori.Kategori
import com.myrent.app.ui.screens.formatRupiah
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.GoldBorderColor
import com.myrent.app.ui.theme.OnAccentGold
import com.myrent.app.ui.theme.StatusGreen
import com.myrent.app.ui.theme.StatusRed
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Komponen reusable Katalog Barang Adaptif & Responsif (Dukungan Multi-Pilih Barang)
 */
@Composable
fun CatalogContent(
    items: List<Item>,
    kategoriList: List<Kategori>,
    selectedKategori: String,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onKategoriSelected: (String) -> Unit,
    fotoRepository: FotoRepository,
    isSelectMode: Boolean = false,
    selectedItemIds: Set<String> = emptySet(),
    onItemClick: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1000.dp)
        ) {
            // Kolom Pencarian
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Cari nama barang...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cari", tint = TextSecondary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Filter Chips Kategori
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isSelected = selectedKategori == "Semua"
                    FilterChip(
                        selected = isSelected,
                        onClick = { onKategoriSelected("Semua") },
                        label = { Text(stringResource(R.string.filter_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold,
                            selectedLabelColor = OnAccentGold,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        )
                    )
                }

                items(kategoriList) { kat ->
                    val isSelected = selectedKategori == kat.nama
                    FilterChip(
                        selected = isSelected,
                        onClick = { onKategoriSelected(kat.nama) },
                        label = { Text(kat.nama) },
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

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Checkroom,
                            contentDescription = "Kosong",
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isSelectMode) "Belum ada barang yang tersedia." else stringResource(R.string.empty_items_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        if (!isSelectMode) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.empty_items_desc),
                                fontSize = 14.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Grid Adaptif: 2 kolom di HP, 3-5 kolom di Tablet
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id }) { item ->
                        val isAvailable = item.stokTersedia > 0 && item.status != "perbaikan"
                        val isSelected = isSelectMode && selectedItemIds.contains(item.id)

                        ReusableItemCard(
                            item = item,
                            fotoRepository = fotoRepository,
                            isSelectMode = isSelectMode,
                            isAvailable = isAvailable,
                            isSelected = isSelected,
                            onClick = {
                                if (!isSelectMode || isAvailable) {
                                    onItemClick(item)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kartu Item Barang Reusable (Mendukung Tanda Centang untuk Multi-Selection)
 */
@Composable
fun ReusableItemCard(
    item: Item,
    fotoRepository: FotoRepository,
    isSelectMode: Boolean,
    isAvailable: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bitmap = remember(item.fotoBase64) {
        if (item.fotoBase64.isNotEmpty()) fotoRepository.base64ToBitmap(item.fotoBase64) else null
    }

    val cardAlpha = if (isSelectMode && !isAvailable) 0.45f else 1.0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable(enabled = !isSelectMode || isAvailable) { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) AccentGold else GoldBorderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = item.nama,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Checkroom,
                        contentDescription = item.nama,
                        tint = TextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Tanda Centang Emas jika Terpilih
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(AccentGold)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Terpilih",
                            tint = OnAccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Badge Status & Stok
                val (badgeBg, badgeText) = when {
                    item.stokTersedia == 0 -> AccentGold to stringResource(R.string.status_rented)
                    item.status == "perbaikan" -> StatusRed to stringResource(R.string.status_maintenance)
                    else -> StatusGreen to "Tersedia (${item.stokTersedia})"
                }

                val textColor = if (item.stokTersedia == 0) OnAccentGold else Color.White

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.nama,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                val subInfo = buildString {
                    append(item.kategori)
                    if (item.ukuran.isNotEmpty()) append(" • ${item.ukuran}")
                    append(" • Stok: ${item.stokTersedia}/${item.stok}")
                }

                Text(
                    text = subInfo,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatRupiah(item.hargaSewa),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold
                )
            }
        }
    }
}
