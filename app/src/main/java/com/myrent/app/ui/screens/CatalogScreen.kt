package com.myrent.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myrent.app.R
import com.myrent.app.data.items.Item
import com.myrent.app.ui.components.AppBackground
import com.myrent.app.ui.katalog.CatalogContent
import com.myrent.app.ui.katalog.CatalogViewModel
import com.myrent.app.ui.katalog.ItemBottomSheet
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.OnAccentGold
import com.myrent.app.ui.theme.TextPrimary
import java.text.NumberFormat
import java.util.Locale

/**
 * Layar Utama Katalog Barang dengan Informasi Stok
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    catalogViewModel: CatalogViewModel = viewModel()
) {
    val items by catalogViewModel.filteredItems.collectAsState()
    val kategoriList by catalogViewModel.kategoriList.collectAsState()
    val selectedKategori by catalogViewModel.selectedKategori.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showBottomSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Item?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        itemToEdit = null
                        catalogViewModel.resetUiState()
                        showBottomSheet = true
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
                            contentDescription = "Tambah Barang",
                            tint = OnAccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_add_item),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = OnAccentGold
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
                Text(
                    text = stringResource(id = R.string.title_catalog),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
                )

                CatalogContent(
                    items = displayedItems,
                    kategoriList = kategoriList,
                    selectedKategori = selectedKategori,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { searchQuery = it },
                    onKategoriSelected = { catalogViewModel.selectKategori(it) },
                    fotoRepository = catalogViewModel.getFotoRepository(),
                    isSelectMode = false,
                    onItemClick = { item ->
                        itemToEdit = item
                        catalogViewModel.resetUiState()
                        showBottomSheet = true
                    }
                )
            }
        }
    }

    if (showBottomSheet) {
        ItemBottomSheet(
            sheetState = sheetState,
            viewModel = catalogViewModel,
            itemToEdit = itemToEdit,
            onDismiss = { showBottomSheet = false }
        )
    }
}

fun formatRupiah(amount: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace(",00", "")
}
