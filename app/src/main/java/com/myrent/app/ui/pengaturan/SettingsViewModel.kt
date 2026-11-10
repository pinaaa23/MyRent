package com.myrent.app.ui.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.myrent.app.data.auth.AuthRepository
import com.myrent.app.data.auth.UserDoc
import com.myrent.app.data.kategori.Kategori
import com.myrent.app.data.kategori.KategoriRepository
import com.myrent.app.data.settings.NotaConfigDoc
import com.myrent.app.data.settings.NotaConfigRepository
import com.myrent.app.data.settings.StoreConfigDoc
import com.myrent.app.data.settings.StoreConfigRepository
import com.myrent.app.data.settings.StoreProfileDoc
import com.myrent.app.data.settings.StoreProfileRepository
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
    private val storeProfileRepository: StoreProfileRepository = StoreProfileRepository(),
    private val notaConfigRepository: NotaConfigRepository = NotaConfigRepository(),
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

    val storeProfile: StateFlow<StoreProfileDoc> = storeProfileRepository.getStoreProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, StoreProfileDoc())

    val notaConfig: StateFlow<NotaConfigDoc> = notaConfigRepository.getNotaConfigFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, NotaConfigDoc())

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            authRepository.getUserProfileFlow().collect { doc ->
                _userDocState.value = doc
                _userName.value = doc.nama
                _userNamaToko.value = doc.namaToko.ifEmpty { "Nama toko belum diisi" }
                _userEmail.value = doc.email
            }
        }
    }

    fun saveFullNotaConfig(
        judulNota: String,
        kalimatPembuka: String,
        catatanBawah: String,
        flagTampilAlamat: Boolean,
        flagTampilTelp: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val res = notaConfigRepository.saveNotaConfig(
                judulNota = judulNota,
                kalimatPembuka = kalimatPembuka,
                catatanBawah = catatanBawah,
                flagTampilAlamat = flagTampilAlamat,
                flagTampilTelp = flagTampilTelp
            )
            res.onSuccess {
                _uiState.value = SettingsUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = SettingsUiState.Error("Gagal menyimpan kustom nota, periksa koneksi internet.")
            }
        }
    }

    fun saveWaFollowUpTemplates(
        templateH1: String,
        templateHariH: String,
        templateTelat: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val res = notaConfigRepository.saveWaTemplates(
                templateH1 = templateH1,
                templateHariH = templateHariH,
                templateTelat = templateTelat
            )
            res.onSuccess {
                _uiState.value = SettingsUiState.Idle
                onSuccess()
            }.onFailure {
                _uiState.value = SettingsUiState.Error("Gagal menyimpan pesan follow-up, periksa koneksi internet.")
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
            }.onFailure { e ->
                val errorMsg = if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                    "Gagal menambah kategori. Silakan periksa koneksi internet atau login ulang."
                } else {
                    e.message ?: "Gagal menambah kategori, periksa koneksi internet."
                }
                _uiState.value = SettingsUiState.Error(errorMsg)
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
            }.onFailure { e ->
                _uiState.value = SettingsUiState.Error(e.message ?: "Gagal mengubah kategori, periksa koneksi internet.")
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
