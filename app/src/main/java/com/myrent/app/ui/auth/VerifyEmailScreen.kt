package com.myrent.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.myrent.app.R
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Layar Verifikasi Email ("Cek Email Kamu")
 * Pengecekan otomatis dilakukan secara SILENT di latar belakang untuk mencegah UI berkedip (flickering).
 */
@Composable
fun VerifyEmailScreen(
    viewModel: AuthViewModel,
    onVerifiedSuccess: () -> Unit,
    onChangeAccount: () -> Unit
) {
    val userEmail = viewModel.getCurrentUserEmail()
    val cooldown by viewModel.resendCooldown.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading

    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Pengecekan silent saat pengguna kembali dari Gmail/Browser ke aplikasi MyRent
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkEmailVerificationStatusSilent(onVerified = onVerifiedSuccess)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 2. Periodic Silent Auto-Check setiap 3 detik (tanpa indikator loading agar tidak berkedip)
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(3000)
            viewModel.checkEmailVerificationStatusSilent(onVerified = onVerifiedSuccess)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.title_verify_email),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.desc_verify_email, userEmail),
            fontSize = 15.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Pesan Sukses / Error jika ada
        if (uiState is AuthUiState.Success) {
            val msgRes = (uiState as AuthUiState.Success).messageResId
            if (msgRes != null) {
                Text(
                    text = stringResource(id = msgRes),
                    color = Color(0xFF6BCB77),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        } else if (uiState is AuthUiState.Error) {
            val errorRes = (uiState as AuthUiState.Error).messageResId
            Text(
                text = stringResource(id = errorRes),
                color = Color(0xFFFF6B6B),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Tombol Saya Sudah Verifikasi (Pengecekan Manual)
        Button(
            onClick = {
                viewModel.checkEmailVerificationStatus(
                    onVerified = onVerifiedSuccess,
                    onNotVerified = {}
                )
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = DarkBackground,
                disabledContainerColor = DarkSurfaceVariant,
                disabledContentColor = TextSecondary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = DarkBackground,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(id = R.string.btn_i_have_verified),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tombol Kirim Ulang Email (dengan Cooldown 30 detik)
        OutlinedButton(
            onClick = {
                viewModel.resendEmailVerification()
            },
            enabled = !isLoading && cooldown == 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AccentGold,
                disabledContentColor = TextSecondary
            )
        ) {
            val btnText = if (cooldown > 0) {
                stringResource(id = R.string.btn_resend_email_cooldown, cooldown)
            } else {
                stringResource(id = R.string.btn_resend_email)
            }
            Text(
                text = btnText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Ganti Akun
        TextButton(onClick = {
            viewModel.logout()
            onChangeAccount()
        }) {
            Text(
                text = stringResource(id = R.string.btn_change_account),
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}
