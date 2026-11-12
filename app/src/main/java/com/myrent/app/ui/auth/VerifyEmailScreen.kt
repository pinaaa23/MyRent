package com.myrent.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.myrent.app.ui.components.AppBackground
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.GoldBorderColor
import com.myrent.app.ui.theme.OnAccentGold
import com.myrent.app.ui.theme.StatusRed
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Layar Verifikasi Email ("Cek Email Kamu") dengan Panduan Visual Cek Folder SPAM
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
    val scrollState = rememberScrollState()

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

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(3000)
            viewModel.checkEmailVerificationStatusSilent(onVerified = onVerifiedSuccess)
        }
    }

    AppBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 520.dp)
                        .padding(24.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.title_verify_email),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(id = R.string.desc_verify_email, userEmail),
                        fontSize = 14.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // KARTU PANDUAN VISUAL KOTAK MASUK -> SPAM
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GoldBorderColor),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Ilustrasi Ikon Email -> Panah -> Folder SPAM
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceVariant)
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.MarkEmailUnread,
                                        contentDescription = "Email",
                                        tint = AccentGold,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Kotak Masuk", fontSize = 11.sp, color = TextSecondary)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Petunjuk Panah",
                                    tint = AccentGold,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderSpecial,
                                        contentDescription = "Folder Spam",
                                        tint = StatusRed,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Folder SPAM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Tata Cara Membuka Email Verifikasi:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "1. Buka aplikasi Gmail / Email di HP Anda.\n" +
                                       "2. Cek Kotak Masuk (Inbox). Jika tidak ada, buka menu samping ☰ lalu pilih folder SPAM.\n" +
                                       "3. Buka email dari MyRent, lalu ketuk tautan verifikasi.",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

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
                            color = StatusRed,
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = OnAccentGold,
                            disabledContainerColor = DarkSurfaceVariant,
                            disabledContentColor = TextSecondary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = OnAccentGold,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.btn_i_have_verified),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnAccentGold
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
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldBorderColor),
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
                            fontWeight = FontWeight.Medium,
                            color = if (cooldown == 0) AccentGold else TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

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
        }
    }
}
