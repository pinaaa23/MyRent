package com.myrent.app.ui.sewa

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.data.items.Item
import com.myrent.app.ui.components.AppBackground
import com.myrent.app.ui.katalog.CatalogContent
import com.myrent.app.ui.katalog.CatalogViewModel
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.OnAccentGold
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * LANGKAH 1 - Layar Penuh "Pilih Barang" (Dukungan Multi-Pilih hingga 10 Barang)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilihBarangScreen(
    catalogViewModel: CatalogViewModel,
    initialSelectedItems: List<Item> = emptyList(),
    onBack: () -> Unit,
    onBarangDipilih: (List<Item>) -> Unit
) {
    val context = LocalContext.current
    val items by catalogViewModel.filteredItems.collectAsState()
    val kategoriList by catalogViewModel.kategoriList.collectAsState()
    val selectedKategori by catalogViewModel.selectedKategori.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val selectedItems = remember { mutableStateListOf<Item>().apply { addAll(initialSelectedItems) } }

    val displayedItems = remember(items, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            items
        } else {
            val q = searchQuery.trim().lowercase()
            items.filter { it.nama.lowercase().contains(q) }
        }
    }

    AppBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pilih Barang (${selectedItems.size}/10)", color = TextPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            if (selectedItems.isNotEmpty()) {
                                onBarangDipilih(selectedItems.toList())
                            }
                        },
                        enabled = selectedItems.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = OnAccentGold,
                            disabledContainerColor = DarkSurfaceVariant,
                            disabledContentColor = TextSecondary
                        )
                    ) {
                        Text(
                            text = if (selectedItems.isNotEmpty()) "Pilih (${selectedItems.size} barang)" else "Pilih Barang",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedItems.isNotEmpty()) OnAccentGold else TextSecondary
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
                CatalogContent(
                    items = displayedItems,
                    kategoriList = kategoriList,
                    selectedKategori = selectedKategori,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { searchQuery = it },
                    onKategoriSelected = { catalogViewModel.selectKategori(it) },
                    fotoRepository = catalogViewModel.getFotoRepository(),
                    isSelectMode = true,
                    selectedItemIds = selectedItems.map { it.id }.toSet(),
                    onItemClick = { item ->
                        val existingIndex = selectedItems.indexOfFirst { it.id == item.id }
                        if (existingIndex >= 0) {
                            selectedItems.removeAt(existingIndex)
                        } else {
                            if (selectedItems.size >= 10) {
                                Toast.makeText(context, "Maksimal 10 barang per transaksi sewa.", Toast.LENGTH_SHORT).show()
                            } else {
                                selectedItems.add(item)
                            }
                        }
                    }
                )
            }
        }
    }
}
