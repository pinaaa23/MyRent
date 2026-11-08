package com.myrent.app.ui.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.myrent.app.data.auth.AuthRepository
import com.myrent.app.data.cabang.Cabang
import com.myrent.app.data.cabang.CabangRepository
import com.myrent.app.data.kategori.Kategori
import com.myrent.app.data.kategori.KategoriRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class SettingsUiState {
    object Idle : SettingsUiState()
    object Loading : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

/**
 * ViewModel untuk mengelola Layar Pengaturan, Kelola Cabang, dan Kelola Kategori
 */
class SettingsViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val cabangRepository: CabangRepository = CabangRepository(),
    private val kategoriRepository: KategoriRepository = KategoriRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userCabangNama = MutableStateFlow("")
    val userCabangNama: StateFlow<String> = _userCabangNama.asStateFlow()

    val cabangList: StateFlow<List<Cabang>> = cabangRepository.getDaftarCabangFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val kategoriList: StateFlow<List<Kategori>> = kategoriRepository.getKategoriFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadUserProfileAndCabang()
    }

    fun loadUserProfileAndCabang() {
        viewModelScope.launch {
            val profileRes = authRepository.getUserProfile()
            profileRes.onSuccess { userDoc ->
                _userName.value = userDoc.nama
                _userEmail.value = userDoc.email
                if (userDoc.cabangId.isNotEmpty()) {
                    val cabangRes = cabangRepository.getCabangById(userDoc.cabangId)
                    cabangRes.onSuccess { cabang ->
                        _userCabangNama.value = cabang.nama
                    }
                }
            }
        }
    }

    fun tambahCabang(nama: String, alamat: String, noWa: String, onSuccess: () -> Unit) {
        if (nama.trim().isEmpty()) {
            _uiState.value = SettingsUiState.Error("Nama cabang tidak boleh kosong.")
            return
        }
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val res = cabangRepository.tambahCabang(Cabang(nama = nama, alamat = alamat, noWa = noWa))
            res.onSuccess {
                _uiState.value = SettingsUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = SettingsUiState.Error("Gagal menambah cabang, periksa koneksi internet.")
            }
        }
    }

    fun updateCabang(id: String, nama: String, alamat: String, noWa: String, onSuccess: () -> Unit) {
        if (nama.trim().isEmpty()) {
            _uiState.value = SettingsUiState.Error("Nama cabang tidak boleh kosong.")
            return
        }
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val res = cabangRepository.updateCabang(Cabang(id = id, nama = nama, alamat = alamat, noWa = noWa))
            res.onSuccess {
                _uiState.value = SettingsUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = SettingsUiState.Error("Gagal mengubah cabang, periksa koneksi internet.")
            }
        }
    }

    fun tambahKategori(nama: String, onSuccess: () -> Unit) {
        if (nama.trim().isEmpty()) {
            _uiState.value = SettingsUiState.Error("Nama kategori tidak boleh kosong.")
            return
        }
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val res = kategoriRepository.tambahKategori(nama)
            res.onSuccess {
                _uiState.value = SettingsUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = SettingsUiState.Error("Gagal menambah kategori, periksa koneksi internet.")
            }
        }
    }

    fun editKategori(id: String, namaBaru: String, onSuccess: () -> Unit) {
        if (namaBaru.trim().isEmpty()) {
            _uiState.value = SettingsUiState.Error("Nama kategori tidak boleh kosong.")
            return
        }
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val res = kategoriRepository.editKategori(id, namaBaru)
            res.onSuccess {
                _uiState.value = SettingsUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = SettingsUiState.Error("Gagal mengubah kategori, periksa koneksi internet.")
            }
        }
    }

    fun hapusKategori(kategori: Kategori, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading

            // Periksa apakah ada barang di koleksi items yang menggunakan nama kategori ini
            val itemsSnapshot = firestore.collection("items")
                .whereEqualTo("kategori", kategori.nama)
                .limit(1)
                .get()
                .await()

            if (!itemsSnapshot.isEmpty) {
                _uiState.value = SettingsUiState.Error("Kategori \"${kategori.nama}\" tidak dapat dihapus karena masih digunakan oleh barang.")
                return@launch
            }

            val res = kategoriRepository.hapusKategori(kategori.id)
            res.onSuccess {
                _uiState.value = SettingsUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = SettingsUiState.Error("Gagal menghapus kategori, periksa koneksi internet.")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = SettingsUiState.Idle
    }

    fun logout() {
        authRepository.logout()
    }
}
