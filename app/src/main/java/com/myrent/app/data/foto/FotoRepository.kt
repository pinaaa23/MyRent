package com.myrent.app.data.foto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Repository pengelola kompresi dan konversi foto barang
 * Mengubah gambar ke/dari format Base64 secara cepat menggunakan Dispatchers.IO
 */
class FotoRepository {

    /**
     * Membaca Uri foto dari Photo Picker, mengompresi maksimal 800px dan JPEG 70%,
     * lalu mengubahnya menjadi String Base64 secara cepat di latar belakang (Dispatchers.IO).
     */
    suspend fun compressUriToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return@withContext null
            inputStream?.close()

            // Hitung skala resize maksimal 800px untuk sisi terpanjang
            val maxDimension = 800
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > height) {
                maxDimension.toFloat() / width
            } else {
                maxDimension.toFloat() / height
            }

            val resizedBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            // Kompresi ke JPEG dengan kualitas 70%
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val imageBytes = outputStream.toByteArray()

            Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Mengubah String Base64 kembali menjadi Bitmap untuk ditampilkan di Compose Image
     */
    fun base64ToBitmap(base64Str: String): Bitmap? {
        if (base64Str.isEmpty()) return null
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
