package com.myrent.app.ui.pengaturan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.myrent.app.data.auth.AuthRepository
import com.myrent.app.data.foto.FotoRepository
import com.myrent.app.data.settings.LocalStoreProfileCache
import com.myrent.app.data.settings.StoreProfileDoc
import com.myrent.app.data.settings.StoreProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlin.math.min

sealed class StoreProfileUiState {
    object Idle : StoreProfileUiState()
    object Loading : StoreProfileUiState()
    data class Success(val message: String) : StoreProfileUiState()
    data class Error(val message: String) : StoreProfileUiState()
}

/**
 * ViewModel untuk mengelola Layar Profil Toko (toko/{uid}/pengaturan/profil) & Cache Lokal Splash Screen
 */
class StoreProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val storeProfileRepository: StoreProfileRepository = StoreProfileRepository(),
    private val fotoRepository: FotoRepository = FotoRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoreProfileUiState>(StoreProfileUiState.Idle)
    val uiState: StateFlow<StoreProfileUiState> = _uiState.asStateFlow()

    private val _storeProfileState = MutableStateFlow(StoreProfileDoc())
    val storeProfileState: StateFlow<StoreProfileDoc> = _storeProfileState.asStateFlow()

    private val currentUid: String?
        get() = auth.currentUser?.uid

    init {
        loadStoreProfile()
    }

    fun loadStoreProfile(context: Context? = null) {
        if (currentUid.isNullOrEmpty()) {
            _uiState.value = StoreProfileUiState.Idle
            return
        }

        viewModelScope.launch {
            _uiState.value = StoreProfileUiState.Loading

            val profileRes = storeProfileRepository.getStoreProfile()
            val profile = profileRes.getOrDefault(StoreProfileDoc())

            val finalNama = if (profile.nama.isEmpty()) {
                val userRes = authRepository.getUserProfile()
                val userDoc = userRes.getOrNull()
                userDoc?.namaToko?.ifEmpty { userDoc.nama } ?: ""
            } else {
                profile.nama
            }

            val finalProfile = profile.copy(nama = finalNama)
            _storeProfileState.value = finalProfile
            _uiState.value = StoreProfileUiState.Idle

            if (context != null) {
                LocalStoreProfileCache.saveCache(context, finalProfile.nama, finalProfile.fotoBase64)
            }
        }
    }

    suspend fun compressAndCropLogoUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val reqDim = 256

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            options.inSampleSize = calculateInSampleSize(options, reqDim, reqDim)
            options.inJustDecodeBounds = false

            inputStream = context.contentResolver.openInputStream(uri)
            val sampledBitmap = BitmapFactory.decodeStream(inputStream, null, options) ?: return@withContext null
            inputStream?.close()

            val rotatedBitmap = try {
                val exifStream = context.contentResolver.openInputStream(uri)
                val exif = exifStream?.let { ExifInterface(it) }
                exifStream?.close()
                val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(sampledBitmap, 90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(sampledBitmap, 180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(sampledBitmap, 270f)
                    else -> sampledBitmap
                }
            } catch (e: Exception) {
                sampledBitmap
            }

            val minEdge = min(rotatedBitmap.width, rotatedBitmap.height)
            val cropX = (rotatedBitmap.width - minEdge) / 2
            val cropY = (rotatedBitmap.height - minEdge) / 2
            val squareBitmap = Bitmap.createBitmap(rotatedBitmap, cropX, cropY, minEdge, minEdge)

            val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, 256, 256, true)

            val baos = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun saveStoreProfile(
        context: Context,
        namaToko: String,
        noTelp: String,
        alamat: String,
        fotoBase64: String,
        onSuccess: () -> Unit
    ) {
        val namaClean = namaToko.trim()
        val telpClean = noTelp.trim()
        val alamatClean = alamat.trim()

        if (namaClean.isEmpty()) {
            _uiState.value = StoreProfileUiState.Error("Nama toko wajib diisi.")
            return
        }

        if (telpClean.isNotEmpty()) {
            val phoneDigits = telpClean.filter { it.isDigit() }
            if (phoneDigits.length !in 8..15) {
                _uiState.value = StoreProfileUiState.Error("Nomor telepon toko harus 8 sampai 15 digit.")
                return
            }
        }

        if (currentUid.isNullOrEmpty()) {
            _uiState.value = StoreProfileUiState.Error("Sesi login berakhir. Silakan login kembali.")
            return
        }

        viewModelScope.launch {
            _uiState.value = StoreProfileUiState.Loading

            val profileDoc = StoreProfileDoc(
                nama = namaClean,
                noTelp = telpClean,
                alamat = alamatClean,
                fotoBase64 = fotoBase64,
                updatedAt = System.currentTimeMillis()
            )

            val result = storeProfileRepository.saveStoreProfile(profileDoc)
            result.onSuccess {
                _storeProfileState.value = profileDoc
                // Simpan salinan lokal khusus untuk SplashScreen
                LocalStoreProfileCache.saveCache(context, namaClean, fotoBase64)

                _uiState.value = StoreProfileUiState.Success("Profil toko berhasil diperbarui.")
                onSuccess()
            }.onFailure { e ->
                _uiState.value = StoreProfileUiState.Error(e.message ?: "Gagal menyimpan profil toko, periksa koneksi internet.")
            }
        }
    }

    fun createTempCameraFile(context: Context): File {
        val timeStamp = System.currentTimeMillis()
        val storageDir = context.cacheDir
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    fun getFotoRepository(): FotoRepository = fotoRepository

    fun resetUiState() {
        _uiState.value = StoreProfileUiState.Idle
    }
}
