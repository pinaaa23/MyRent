package com.myrent.app.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.data.settings.LocalStoreCache
import com.myrent.app.data.settings.LocalStoreProfileCache
import com.myrent.app.data.settings.StoreProfileRepository
import com.myrent.app.ui.components.AppBackground
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.TextSecondary
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

/**
 * Layar Splash di dalam Aplikasi saat Pengguna Sudah Login
 * Menampilkan ucapan "Selamat datang kembali," & Nama Toko tanpa ikon.
 */
@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    onNavigateToMain: () -> Unit,
    onNavigateToVerifyEmail: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    var localCache by remember { mutableStateOf(LocalStoreCache()) }
    var shouldShowInAppSplash by remember { mutableStateOf(false) }
    val alphaAnim = remember { Animatable(0f) }
    val storeProfileRepo = remember { StoreProfileRepository() }

    LaunchedEffect(Unit) {
        val (isLoggedIn, isVerified) = viewModel.isUserLoggedInAndVerified()

        if (!isLoggedIn) {
            onNavigateToLogin()
            return@LaunchedEffect
        }

        if (!isVerified) {
            onNavigateToVerifyEmail()
            return@LaunchedEffect
        }

        var cache = LocalStoreProfileCache.readCache(context)

        if (cache.namaToko.isEmpty()) {
            val storeProfileRes = storeProfileRepo.getStoreProfile()
            val userProfileRes = viewModel.getUserProfile()

            val storeProfile = storeProfileRes.getOrNull()
            val userDoc = userProfileRes.getOrNull()

            val tokoNama = storeProfile?.nama?.ifEmpty { userDoc?.namaToko }?.ifEmpty { "Kusuma Rent" } ?: "Kusuma Rent"
            val foto = storeProfile?.fotoBase64?.ifEmpty { userDoc?.fotoProfilBase64 ?: "" } ?: ""

            cache = LocalStoreCache(namaToko = tokoNama, fotoBase64 = foto)
            LocalStoreProfileCache.saveCache(context, tokoNama, foto)
        }

        localCache = cache
        shouldShowInAppSplash = true

        val animJob = async {
            alphaAnim.animateTo(1f, animationSpec = tween(400))
            delay(600)
        }

        animJob.await()
        onNavigateToMain()
    }

    if (!shouldShowInAppSplash) return

    AppBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.alpha(alphaAnim.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Selamat datang kembali,",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = localCache.namaToko.ifEmpty { "MyRent" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
