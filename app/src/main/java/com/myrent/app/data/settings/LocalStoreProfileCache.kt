package com.myrent.app.data.settings

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class LocalStoreCache(
    val namaToko: String = "",
    val fotoBase64: String = ""
)

/**
 * Penyimpanan lokal aman (JSON File di filesDir) khusus untuk SplashScreen
 * TIDAK memanggil Firestore sama sekali.
 */
object LocalStoreProfileCache {

    private const val FILE_NAME = "store_profile_cache.json"

    suspend fun saveCache(context: Context, namaToko: String, fotoBase64: String) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("namaToko", namaToko.trim())
                put("fotoBase64", fotoBase64.trim())
                put("updatedAt", System.currentTimeMillis())
            }
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun readCache(context: Context): LocalStoreCache = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return@withContext LocalStoreCache()

            val text = file.readText()
            if (text.isEmpty()) return@withContext LocalStoreCache()

            val json = JSONObject(text)
            LocalStoreCache(
                namaToko = json.optString("namaToko", ""),
                fotoBase64 = json.optString("fotoBase64", "")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            LocalStoreCache()
        }
    }

    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
