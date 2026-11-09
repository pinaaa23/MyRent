package com.myrent.app.ui.pengaturan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Patterns
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myrent.app.data.auth.AuthRepository
import com.myrent.app.data.auth.UserDoc
import com.myrent.app.data.customer.normalizeNoWa
import com.myrent.app.data.foto.FotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

sealed class EditProfileUiState {
    object Idle : EditProfileUiState()
    object Loading : EditProfileUiState()
    data class Success(val message: String) : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}

/**
 * ViewModel untuk mengelola Layar Edit Profil Pemilik & Toko
 */
class EditProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val fotoRepository: FotoRepository = FotoRepository(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _userDocState = MutableStateFlow(UserDoc())
    val userDocState: StateFlow<UserDoc> = _userDocState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            authRepository.getUserProfileFlow().collect { doc ->
                _userDocState.value = doc
            }
        }
    }

    /**
     * Memproses gambar dari Uri dengan inSampleSize hemat memori, memutar EXIF jika miring,
     * mengompresi maksimal 400px JPEG 70%, dan mengubahnya ke Base64 di Dispatchers.IO
     */
    suspend fun compressLogoUriToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val maxDim = 400

            // Step 1: Decode bounds
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            options.inSampleSize = calculateInSampleSize(options, maxDim, maxDim)
            options.inJustDecodeBounds = false

            // Step 2: Decode sampled bitmap
            inputStream = context.contentResolver.openInputStream(uri)
            val sampledBitmap = BitmapFactory.decodeStream(inputStream, null, options) ?: return@withContext null
            inputStream?.close()

            // Step 3: Correct EXIF Rotation
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

            // Step 4: Scale
            val width = rotatedBitmap.width
            val height = rotatedBitmap.height
            val scale = if (width > height) maxDim.toFloat() / width else maxDim.toFloat() / height

            val resized = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(rotatedBitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                rotatedBitmap
            }

            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos)
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

    fun saveProfile(
        namaPemilik: String,
        namaToko: String,
        noWaTokoRaw: String,
        alamatToko: String,
        emailInput: String,
        fotoBase64: String,
        onSuccess: () -> Unit
    ) {
        val nama = namaPemilik.trim()
        val toko = namaToko.trim()
        val alamat = alamatToko.trim()
        val email = emailInput.trim()

        if (nama.isEmpty() || toko.isEmpty() || email.isEmpty()) {
            _uiState.value = EditProfileUiState.Error("Nama pemilik, Nama toko, dan Email wajib diisi.")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = EditProfileUiState.Error("Format email tidak valid.")
            return
        }

        val noWaNorm = if (noWaTokoRaw.trim().isNotEmpty()) {
            normalizeNoWa(noWaTokoRaw)
        } else ""

        if (noWaNorm.isNotEmpty() && noWaNorm.length !in 10..14) {
            _uiState.value = EditProfileUiState.Error("Nomor WA toko harus 10 sampai 14 digit.")
            return
        }

        if (alamat.length > 200) {
            _uiState.value = EditProfileUiState.Error("Alamat toko maksimal 200 karakter.")
            return
        }

        val user = auth.currentUser
        if (user == null) {
            _uiState.value = EditProfileUiState.Error("Sesi login berakhir. Silakan login kembali.")
            return
        }

        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Loading

            try {
                // Update email di Firebase Auth jika berubah
                if (email != user.email) {
                    try {
                        user.updateEmail(email).await()
                    } catch (e: Exception) {
                        _uiState.value = EditProfileUiState.Error("Mengubah email memerlukan login ulang. Silakan keluar dan masuk kembali.")
                        return@launch
                    }
                }

                val map = mapOf(
                    "nama" to nama,
                    "namaToko" to toko,
                    "noWaToko" to noWaNorm,
                    "alamatToko" to alamat,
                    "email" to email,
                    "fotoProfilBase64" to fotoBase64
                )

                firestore.collection("users").document(user.uid).update(map).await()
                _userDocState.value = _userDocState.value.copy(
                    nama = nama,
                    namaToko = toko,
                    noWaToko = noWaNorm,
                    alamatToko = alamat,
                    email = email,
                    fotoProfilBase64 = fotoBase64
                )
                _uiState.value = EditProfileUiState.Success("Profil toko berhasil diperbarui.")
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = EditProfileUiState.Error("Gagal menyimpan profil, periksa koneksi internet.")
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
        _uiState.value = EditProfileUiState.Idle
    }
}
