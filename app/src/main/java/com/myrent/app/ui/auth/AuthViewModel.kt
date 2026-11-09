package com.myrent.app.ui.auth

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.myrent.app.R
import com.myrent.app.data.auth.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(@StringRes val messageResId: Int? = null) : AuthUiState()
    data class Error(@StringRes val messageResId: Int) : AuthUiState()
}

/**
 * ViewModel untuk mengelola alur Otentikasi (Login, Register, Verifikasi Email, Lupa Password)
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _resendCooldown = MutableStateFlow(0)
    val resendCooldown: StateFlow<Int> = _resendCooldown.asStateFlow()

    val currentUser: FirebaseUser?
        get() = authRepository.currentUser

    fun getCurrentUserEmail(): String = currentUser?.email ?: ""

    /**
     * Memeriksa status login dan verifikasi email pengguna
     */
    suspend fun isUserLoggedInAndVerified(): Pair<Boolean, Boolean> {
        val user = authRepository.currentUser ?: return Pair(false, false)
        val reloadedRes = authRepository.reloadUser()
        val isVerified = reloadedRes.getOrNull()?.isEmailVerified ?: user.isEmailVerified
        return Pair(true, isVerified)
    }

    /**
     * Alur Login Pengguna dengan Email dan Kata Sandi
     */
    fun login(
        emailInput: String,
        passwordInput: String,
        onSuccess: (isVerified: Boolean) -> Unit
    ) {
        val email = emailInput.trim()
        val password = passwordInput.trim()

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.value = AuthUiState.Error(R.string.error_empty_field)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(email, password)

            result.onSuccess { user ->
                val isVerified = user.isEmailVerified
                _uiState.value = AuthUiState.Idle
                onSuccess(isVerified)
            }.onFailure { exception ->
                _uiState.value = AuthUiState.Error(mapFirebaseErrorToResId(exception))
            }
        }
    }

    /**
     * Alur Pendaftaran Akun Baru (1 Akun = 1 Toko)
     */
    fun registerUser(
        fullNameInput: String,
        storeNameInput: String,
        emailInput: String,
        passwordInput: String,
        confirmPasswordInput: String,
        onSuccess: () -> Unit
    ) {
        val fullName = fullNameInput.trim()
        val storeName = storeNameInput.trim()
        val email = emailInput.trim()
        val password = passwordInput.trim()
        val confirmPassword = confirmPasswordInput.trim()

        if (fullName.isEmpty() || storeName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            _uiState.value = AuthUiState.Error(R.string.error_empty_field)
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = AuthUiState.Error(R.string.error_invalid_email)
            return
        }

        if (password.length < 6) {
            _uiState.value = AuthUiState.Error(R.string.error_short_password)
            return
        }

        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error(R.string.error_password_mismatch)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.register(
                nama = fullName,
                namaToko = storeName,
                email = email,
                password = password
            )

            result.onSuccess {
                _uiState.value = AuthUiState.Idle
                onSuccess()
            }.onFailure { exception ->
                _uiState.value = AuthUiState.Error(mapFirebaseErrorToResId(exception))
            }
        }
    }

    /**
     * Mengirim ulang email verifikasi
     */
    fun resendEmailVerification() {
        if (_resendCooldown.value > 0) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.resendEmailVerification()

            result.onSuccess {
                _uiState.value = AuthUiState.Success(R.string.email_sent_success)
                startResendCooldown()
            }.onFailure { exception ->
                _uiState.value = AuthUiState.Error(mapFirebaseErrorToResId(exception))
            }
        }
    }

    private fun startResendCooldown() {
        viewModelScope.launch {
            _resendCooldown.value = 30
            while (_resendCooldown.value > 0) {
                delay(1000)
                _resendCooldown.value -= 1
            }
        }
    }

    /**
     * Pengecekan status verifikasi email secara silent di background
     */
    fun checkEmailVerificationStatusSilent(onVerified: () -> Unit) {
        viewModelScope.launch {
            val reloadedRes = authRepository.reloadUser()
            val isVerified = reloadedRes.getOrNull()?.isEmailVerified ?: false
            if (isVerified) {
                onVerified()
            }
        }
    }

    /**
     * Pengecekan status verifikasi email secara manual
     */
    fun checkEmailVerificationStatus(
        onVerified: () -> Unit,
        onNotVerified: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val reloadedRes = authRepository.reloadUser()
            val isVerified = reloadedRes.getOrNull()?.isEmailVerified ?: false

            _uiState.value = AuthUiState.Idle
            if (isVerified) {
                onVerified()
            } else {
                _uiState.value = AuthUiState.Error(R.string.email_not_verified)
                onNotVerified()
            }
        }
    }

    /**
     * Mengirimkan email reset kata sandi
     */
    fun sendPasswordResetEmail(
        emailInput: String,
        onSuccess: () -> Unit
    ) {
        val email = emailInput.trim()

        if (email.isEmpty()) {
            _uiState.value = AuthUiState.Error(R.string.error_empty_field)
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = AuthUiState.Error(R.string.error_invalid_email)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.sendPasswordResetEmail(email)

            result.onSuccess {
                _uiState.value = AuthUiState.Success(R.string.reset_link_sent_success)
                onSuccess()
            }.onFailure { exception ->
                _uiState.value = AuthUiState.Error(mapFirebaseErrorToResId(exception))
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState.Idle
    }

    fun resetUiState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun mapFirebaseErrorToResId(exception: Throwable): Int {
        return when (exception) {
            is FirebaseAuthUserCollisionException -> R.string.error_email_already_used
            is FirebaseAuthInvalidCredentialsException -> R.string.error_wrong_credentials
            else -> R.string.error_unknown
        }
    }
}
