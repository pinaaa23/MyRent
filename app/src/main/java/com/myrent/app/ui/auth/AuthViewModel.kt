package com.myrent.app.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.myrent.app.R
import com.myrent.app.data.auth.AuthRepository
import com.myrent.app.data.cabang.Cabang
import com.myrent.app.data.cabang.CabangRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State umum untuk UI Otentikasi
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val messageResId: Int? = null) : AuthUiState()
    data class Error(val messageResId: Int) : AuthUiState()
}

/**
 * ViewModel pengelola aliran data dan logika bisnis Otentikasi & Akun
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val cabangRepository: CabangRepository = CabangRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _daftarCabangState = MutableStateFlow<List<Cabang>>(emptyList())
    val daftarCabangState: StateFlow<List<Cabang>> = _daftarCabangState.asStateFlow()

    private val _isLoadingCabang = MutableStateFlow(false)
    val isLoadingCabang: StateFlow<Boolean> = _isLoadingCabang.asStateFlow()

    private val _resendCooldown = MutableStateFlow(0)
    val resendCooldown: StateFlow<Int> = _resendCooldown.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        loadDaftarCabang()
    }

    /**
     * Memuat daftar cabang dari Firestore untuk pendaftaran
     */
    fun loadDaftarCabang() {
        viewModelScope.launch {
            _isLoadingCabang.value = true
            val result = cabangRepository.getDaftarCabang()
            result.onSuccess { cabangList ->
                _daftarCabangState.value = cabangList
            }.onFailure {
                _daftarCabangState.value = emptyList()
            }
            _isLoadingCabang.value = false
        }
    }

    /**
     * Login pengguna
     */
    fun login(email: String, password: String, onSuccess: (isVerified: Boolean) -> Unit) {
        val emailTrim = email.trim()
        val passwordTrim = password.trim()

        if (emailTrim.isEmpty() || passwordTrim.isEmpty()) {
            _uiState.value = AuthUiState.Error(R.string.error_empty_field)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()) {
            _uiState.value = AuthUiState.Error(R.string.error_invalid_email)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(emailTrim, passwordTrim)
            result.onSuccess { user ->
                _uiState.value = AuthUiState.Idle
                onSuccess(user.isEmailVerified)
            }.onFailure { exception ->
                _uiState.value = AuthUiState.Error(mapFirebaseException(exception))
            }
        }
    }

    /**
     * Pendaftaran akun baru
     */
    fun register(
        nama: String,
        email: String,
        password: String,
        confirmPassword: String,
        selectedCabang: Cabang?,
        onSuccess: () -> Unit
    ) {
        val namaTrim = nama.trim()
        val emailTrim = email.trim()
        val passwordTrim = password.trim()
        val confirmTrim = confirmPassword.trim()

        if (namaTrim.isEmpty() || emailTrim.isEmpty() || passwordTrim.isEmpty() || confirmTrim.isEmpty()) {
            _uiState.value = AuthUiState.Error(R.string.error_empty_field)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()) {
            _uiState.value = AuthUiState.Error(R.string.error_invalid_email)
            return
        }
        if (passwordTrim.length < 6) {
            _uiState.value = AuthUiState.Error(R.string.error_short_password)
            return
        }
        if (passwordTrim != confirmTrim) {
            _uiState.value = AuthUiState.Error(R.string.error_password_mismatch)
            return
        }
        if (selectedCabang == null) {
            _uiState.value = AuthUiState.Error(R.string.error_select_cabang)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.register(
                nama = namaTrim,
                email = emailTrim,
                password = passwordTrim,
                cabangId = selectedCabang.id
            )
            result.onSuccess {
                _uiState.value = AuthUiState.Idle
                onSuccess()
            }.onFailure { exception ->
                _uiState.value = AuthUiState.Error(mapFirebaseException(exception))
            }
        }
    }

    /**
     * Kirim ulang email verifikasi dengan cooldown 30 detik
     */
    fun resendEmailVerification() {
        if (_resendCooldown.value > 0) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.resendEmailVerification()
            result.onSuccess {
                _uiState.value = AuthUiState.Success(R.string.email_sent_success)
                startCooldownTimer()
            }.onFailure { exception ->
                _uiState.value = AuthUiState.Error(mapFirebaseException(exception))
            }
        }
    }

    /**
     * Memeriksa verifikasi email pengguna saat menekan "Saya sudah verifikasi" secara manual (menampilkan indikator loading)
     */
    fun checkEmailVerificationStatus(onVerified: () -> Unit, onNotVerified: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.reloadUser()
            result.onSuccess { user ->
                _uiState.value = AuthUiState.Idle
                if (user.isEmailVerified) {
                    onVerified()
                } else {
                    onNotVerified()
                }
            }.onFailure { exception ->
                _uiState.value = AuthUiState.Error(mapFirebaseException(exception))
            }
        }
    }

    /**
     * Memeriksa verifikasi email pengguna secara SILENT (latar belakang) tanpa mengubah uiState (mencegah flickering/berkedip)
     */
    fun checkEmailVerificationStatusSilent(onVerified: () -> Unit) {
        viewModelScope.launch {
            val result = authRepository.reloadUser()
            result.onSuccess { user ->
                if (user.isEmailVerified) {
                    _uiState.value = AuthUiState.Idle
                    onVerified()
                }
            }
        }
    }

    /**
     * Kirim tautan reset kata sandi
     */
    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit) {
        val emailTrim = email.trim()
        if (emailTrim.isEmpty()) {
            _uiState.value = AuthUiState.Error(R.string.error_empty_field)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()) {
            _uiState.value = AuthUiState.Error(R.string.error_invalid_email)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.sendPasswordResetEmail(emailTrim)
            result.onSuccess {
                _uiState.value = AuthUiState.Success(R.string.reset_link_sent_success)
                onSuccess()
            }.onFailure { exception ->
                _uiState.value = AuthUiState.Error(mapFirebaseException(exception))
            }
        }
    }

    /**
     * Logout pengguna
     */
    fun logout() {
        authRepository.logout()
    }

    fun getCurrentUserEmail(): String {
        return authRepository.currentUser?.email ?: ""
    }

    fun isUserLoggedInAndVerified(): Pair<Boolean, Boolean> {
        val user = authRepository.currentUser
        return Pair(user != null, user?.isEmailVerified == true)
    }

    fun resetUiState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun startCooldownTimer() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (i in 30 downTo 1) {
                _resendCooldown.value = i
                delay(1000)
            }
            _resendCooldown.value = 0
        }
    }

    /**
     * Memetakan exception Firebase ke teks pesan Bahasa Indonesia
     */
    private fun mapFirebaseException(exception: Throwable): Int {
        return when (exception) {
            is FirebaseAuthUserCollisionException -> R.string.error_email_already_used
            is FirebaseAuthInvalidCredentialsException,
            is FirebaseAuthInvalidUserException -> R.string.error_wrong_credentials
            is FirebaseNetworkException -> R.string.error_no_network
            is FirebaseTooManyRequestsException -> R.string.error_too_many_requests
            else -> R.string.error_unknown
        }
    }
}
