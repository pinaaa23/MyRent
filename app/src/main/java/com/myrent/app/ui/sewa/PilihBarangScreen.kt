package com.myrent.app.ui.sewa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.data.items.Item
import com.myrent.app.ui.katalog.CatalogContent
import com.myrent.app.ui.katalog.CatalogViewModel
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * LANGKAH 1 - Layar Penuh "Pilih Barang" dari Katalog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilihBarangScreen(
    catalogViewModel: CatalogViewModel,
    initialSelectedItem: Item? = null,
    onBack: () -> Unit,
    onBarangDipilih: (Item) -> Unit
) {
    val items by catalogViewModel.filteredItems.collectAsState()
    val kategoriList by catalogViewModel.kategoriList.collectAsState()
    val selectedKategori by catalogViewModel.selectedKategori.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<Item?>(initialSelectedItem) }

    // Filter lokal berdasarkan pencarian nama barang
    val displayedItems = remember(items, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            items
        } else {
            val q = searchQuery.trim().lowercase()
            items.filter { it.nama.lowercase().contains(q) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pilih Barang", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        selectedItem?.let { onBarangDipilih(it) }
                    },
                    enabled = selectedItem != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = DarkBackground,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = TextSecondary
                    )
                ) {
                    Text(
                        text = if (selectedItem != null) "Pilih (${selectedItem!!.nama})" else "Pilih Barang",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
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
                selectedItemId = selectedItem?.id ?: "",
                onItemClick = { item ->
                    selectedItem = item
                }
            )
        }
    }
}
