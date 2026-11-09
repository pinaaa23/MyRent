package com.myrent.app.ui.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.myrent.app.data.auth.AuthRepository
import com.myrent.app.data.auth.UserDoc
import com.myrent.app.data.kategori.Kategori
import com.myrent.app.data.kategori.KategoriRepository
import com.myrent.app.data.settings.StoreConfigDoc
import com.myrent.app.data.settings.StoreConfigRepository
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
 * ViewModel untuk mengelola Layar Pengaturan, Profil Toko, Kustom Nota, & Template WA
 */
class SettingsViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val kategoriRepository: KategoriRepository = KategoriRepository(),
    private val storeConfigRepository: StoreConfigRepository = StoreConfigRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _userDocState = MutableStateFlow(UserDoc())
    val userDocState: StateFlow<UserDoc> = _userDocState.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userNamaToko = MutableStateFlow("")
    val userNamaToko: StateFlow<String> = _userNamaToko.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    val kategoriList: StateFlow<List<Kategori>> = kategoriRepository.getKategoriFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val storeConfig: StateFlow<StoreConfigDoc> = storeConfigRepository.getStoreConfigFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, StoreConfigDoc())

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val profileRes = authRepository.getUserProfile()
            profileRes.onSuccess { doc ->
                var finalDoc = doc

                // Migrasi 1x Aman untuk akun kusumarefina@gmail.com jika namaToko masih kosong/berisi UID
                if (doc.email.equals("kusumarefina@gmail.com", ignoreCase = true) && (doc.namaToko.isEmpty() || doc.namaToko.length > 15)) {
                    val uid = authRepository.currentUser?.uid
                    if (!uid.isNullOrEmpty()) {
                        try {
                            firestore.collection("users").document(uid).update("namaToko", "Kusuma Rent").await()
                            finalDoc = doc.copy(namaToko = "Kusuma Rent")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                _userDocState.value = finalDoc
                _userName.value = finalDoc.nama
                _userNamaToko.value = finalDoc.namaToko.ifEmpty { "Nama toko belum diisi" }
                _userEmail.value = finalDoc.email
            }
        }
    }

    fun saveNotaConfig(header: String, footer: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val res = storeConfigRepository.saveNotaConfig(header, footer)
            res.onSuccess {
                _uiState.value = SettingsUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = SettingsUiState.Error("Gagal menyimpan kustom nota, periksa koneksi internet.")
            }
        }
    }

    fun saveWaTemplates(
        templateNota: String,
        templateH1: String,
        templateH: String,
        onSuccess: () -> Unit
    ) {
        if (templateNota.trim().isEmpty() || templateH1.trim().isEmpty() || templateH.trim().isEmpty()) {
            _uiState.value = SettingsUiState.Error("Template pesan WhatsApp tidak boleh kosong.")
            return
        }

        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val res = storeConfigRepository.saveWaTemplates(templateNota, templateH1, templateH)
            res.onSuccess {
                _uiState.value = SettingsUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = SettingsUiState.Error("Gagal menyimpan template WhatsApp, periksa koneksi internet.")
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

            val uid = authRepository.currentUser?.uid ?: ""
            val itemsSnapshot = firestore.collection("toko").document(uid).collection("items")
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
