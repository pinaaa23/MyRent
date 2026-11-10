package com.myrent.app.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.data.foto.FotoRepository
import com.myrent.app.data.settings.LocalStoreCache
import com.myrent.app.data.settings.LocalStoreProfileCache
import com.myrent.app.data.settings.StoreProfileRepository
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

/**
 * Layar Splash di dalam Aplikasi dengan Foto Toko & Nama Toko
 * Tampil HANYA jika pengguna sudah login dan memiliki profil toko (masuk ke-2 kali dst).
 * Jika belum login / belum ada profil, langsung dilewati ke layar Login / Beranda tanpa splash kedua yang duplikat.
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
    val fotoRepo = remember { FotoRepository() }
    val storeProfileRepo = remember { StoreProfileRepository() }

    LaunchedEffect(Unit) {
        // Cek status login
        val (isLoggedIn, isVerified) = viewModel.isUserLoggedInAndVerified()

        if (!isLoggedIn) {
            // Belum login: Lewati splash kedua, langsung ke LoginScreen
            onNavigateToLogin()
            return@LaunchedEffect
        }

        if (!isVerified) {
            onNavigateToVerifyEmail()
            return@LaunchedEffect
        }

        // Pengguna sudah login & terverifikasi: Baca salinan profil toko dari lokal
        var cache = LocalStoreProfileCache.readCache(context)

        // Jika cache di disk belum ada, coba muat sekali dari profil toko
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

        // Tampilkan Splash Screen Foto Toko dengan animasi fade-in halus 1 detik
        val animJob = async {
            alphaAnim.animateTo(1f, animationSpec = tween(400))
            delay(600) // Memastikan tampilan minimal 1 detik total
        }

        animJob.await()

        // Langsung masuk ke Beranda tanpa perlu login ulang!
        onNavigateToMain()
    }

    if (!shouldShowInAppSplash) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val bitmapFromBase64 = remember(localCache.fotoBase64) {
                if (localCache.fotoBase64.isNotEmpty()) fotoRepo.base64ToBitmap(localCache.fotoBase64) else null
            }

            // Foto Toko Lingkaran (~120dp)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (bitmapFromBase64 != null) {
                    Image(
                        bitmap = bitmapFromBase64.asImageBitmap(),
                        contentDescription = "Foto Toko Splash",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Store,
                        contentDescription = "Logo MyRent Default",
                        tint = AccentGold,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nama Toko (Huruf Tebal)
            Text(
                text = localCache.namaToko.ifEmpty { "MyRent" },
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (localCache.namaToko.isNotEmpty()) TextPrimary else AccentGold
            )
        }
    }
}
