package com.myrent.app.ui.katalog

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myrent.app.data.foto.FotoRepository
import com.myrent.app.data.items.Item
import com.myrent.app.data.items.ItemRepository
import com.myrent.app.data.kategori.Kategori
import com.myrent.app.data.kategori.KategoriRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CatalogUiState {
    object Idle : CatalogUiState()
    object Loading : CatalogUiState()
    data class Success(val message: String? = null) : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

/**
 * ViewModel untuk mengelola data Katalog, Stok Barang, Kategori Inline, dan formulir Tambah/Edit barang
 */
class CatalogViewModel(
    private val itemRepository: ItemRepository = ItemRepository(),
    private val kategoriRepository: KategoriRepository = KategoriRepository(),
    private val fotoRepository: FotoRepository = FotoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Idle)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private val _selectedKategori = MutableStateFlow("Semua")
    val selectedKategori: StateFlow<String> = _selectedKategori.asStateFlow()

    private val _allItems = MutableStateFlow<List<Item>>(emptyList())

    val kategoriList: StateFlow<List<Kategori>> = kategoriRepository.getKategoriFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // List barang yang difilter berdasarkan chip kategori terpilih
    val filteredItems: StateFlow<List<Item>> = combine(_allItems, _selectedKategori) { items, filter ->
        if (filter == "Semua") items else items.filter { it.kategori == filter }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            itemRepository.getItemsFlow().collect { itemList ->
                _allItems.value = itemList
            }
        }
    }

    fun selectKategori(nama: String) {
        _selectedKategori.value = nama
    }

    /**
     * Menambah kategori baru secara langsung dari formulir Tambah Barang (Inline)
     */
    fun tambahKategoriInline(nama: String, onSuccess: (String) -> Unit) {
        val trimmed = nama.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = CatalogUiState.Error("Nama kategori tidak boleh kosong.")
            return
        }

        viewModelScope.launch {
            _uiState.value = CatalogUiState.Loading
            val result = kategoriRepository.tambahKategori(trimmed)
            result.onSuccess {
                _uiState.value = CatalogUiState.Idle
                onSuccess(trimmed)
            }.onFailure { e ->
                _uiState.value = CatalogUiState.Error(e.message ?: "Gagal menambah kategori.")
            }
        }
    }

    fun simpanItem(
        id: String = "",
        nama: String,
        kategori: String,
        ukuran: String,
        stok: Int,
        hargaSewa: Long,
        fotoBase64Existing: String,
        newPhotoUri: Uri?,
        status: String,
        catatan: String,
        context: Context,
        onSuccess: () -> Unit
    ) {
        if (nama.trim().isEmpty()) {
            _uiState.value = CatalogUiState.Error("Nama barang tidak boleh kosong.")
            return
        }
        if (kategori.trim().isEmpty()) {
            _uiState.value = CatalogUiState.Error("Kategori barang wajib dipilih.")
            return
        }
        if (stok <= 0) {
            _uiState.value = CatalogUiState.Error("Jumlah stok minimal 1.")
            return
        }
        if (hargaSewa <= 0) {
            _uiState.value = CatalogUiState.Error("Harga sewa harus lebih dari 0.")
            return
        }

        viewModelScope.launch {
            _uiState.value = CatalogUiState.Loading

            // Mengompres foto jika pengguna memilih foto baru
            val finalBase64 = if (newPhotoUri != null) {
                fotoRepository.compressUriToBase64(context, newPhotoUri) ?: fotoBase64Existing
            } else {
                fotoBase64Existing
            }

            val itemToSave = Item(
                id = id,
                cabangId = "",
                nama = nama.trim(),
                kategori = kategori.trim(),
                ukuran = ukuran.trim(),
                hargaSewa = hargaSewa,
                stok = stok,
                stokTersedia = if (id.isEmpty()) stok else stok,
                fotoBase64 = finalBase64,
                status = status,
                catatan = catatan.trim(),
                dibuatPada = System.currentTimeMillis()
            )

            val result = if (id.isEmpty()) {
                itemRepository.tambahItem(itemToSave)
            } else {
                itemRepository.updateItem(itemToSave)
            }

            result.onSuccess {
                _uiState.value = CatalogUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = CatalogUiState.Error("Gagal menyimpan barang, periksa koneksi internet.")
            }
        }
    }

    fun hapusItem(item: Item, onSuccess: () -> Unit) {
        if (item.status == "disewa") {
            _uiState.value = CatalogUiState.Error("Barang yang sedang disewa tidak dapat dihapus.")
            return
        }

        viewModelScope.launch {
            _uiState.value = CatalogUiState.Loading
            val result = itemRepository.hapusItem(item.id)
            result.onSuccess {
                _uiState.value = CatalogUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = CatalogUiState.Error("Gagal menghapus barang, periksa koneksi internet.")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = CatalogUiState.Idle
    }

    fun getFotoRepository(): FotoRepository = fotoRepository
}
