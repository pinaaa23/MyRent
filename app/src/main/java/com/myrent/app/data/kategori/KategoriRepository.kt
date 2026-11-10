package com.myrent.app.data.kategori

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pengelola koleksi "kategori" terisolasi per akun toko (toko/{uid}/kategori)
 */
class KategoriRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    private val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified == true

    private fun getKategoriCollection(uid: String = currentUid) =
        firestore.collection("toko").document(uid.ifEmpty { "global" }).collection("kategori")

    private suspend fun refreshTokenIfNeeded() {
        val user = auth.currentUser ?: return
        try {
            user.reload().await()
            user.getIdToken(true).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Mendapatkan daftar kategori secara realtime via Flow milik akun toko (toko/{uid}/kategori).
     */
    fun getKategoriFlow(): Flow<List<Kategori>> = callbackFlow {
        val uid = currentUid
        if (uid.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val userKatRef = getKategoriCollection(uid)

        val listener = userKatRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                val nama = doc.getString("nama") ?: ""
                if (nama.isNotEmpty()) Kategori(id = doc.id, nama = nama) else null
            } ?: emptyList()

            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Menambah kategori baru dengan pencegahan nama ganda & penyegaran token otomatis jika butuh izin
     */
    suspend fun tambahKategori(nama: String): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty()) { "Pengguna belum login" }
        val trimmed = nama.trim()
        require(trimmed.isNotEmpty()) { "Nama kategori tidak boleh kosong." }

        refreshTokenIfNeeded()

        val userKatRef = getKategoriCollection(uid)

        val existingDocs = try {
            userKatRef.get().await()
        } catch (e: Exception) {
            refreshTokenIfNeeded()
            userKatRef.get().await()
        }

        val isDuplicate = existingDocs.documents.any {
            (it.getString("nama") ?: "").equals(trimmed, ignoreCase = true)
        }

        if (isDuplicate) {
            error("Kategori \"$trimmed\" sudah ada.")
        }

        try {
            userKatRef.add(mapOf("nama" to trimmed)).await()
        } catch (e: Exception) {
            refreshTokenIfNeeded()
            userKatRef.add(mapOf("nama" to trimmed)).await()
        }
    }

    /**
     * Mengedit nama kategori dengan pencegahan nama ganda
     */
    suspend fun editKategori(id: String, namaBaru: String): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty()) { "Pengguna belum login" }
        val trimmed = namaBaru.trim()
        require(trimmed.isNotEmpty()) { "Nama kategori tidak boleh kosong." }

        refreshTokenIfNeeded()

        val userKatRef = getKategoriCollection(uid)
        val existingDocs = userKatRef.get().await()
        val isDuplicate = existingDocs.documents.any {
            it.id != id && (it.getString("nama") ?: "").equals(trimmed, ignoreCase = true)
        }

        if (isDuplicate) {
            error("Kategori \"$trimmed\" sudah ada.")
        }

        try {
            userKatRef.document(id).update("nama", trimmed).await()
        } catch (e: Exception) {
            refreshTokenIfNeeded()
            userKatRef.document(id).update("nama", trimmed).await()
        }
    }

    /**
     * Menghapus dokumen kategori dari toko/{uid}/kategori/{id}
     */
    suspend fun hapusKategori(id: String): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty()) { "Pengguna belum login" }

        try {
            getKategoriCollection(uid).document(id).delete().await()
        } catch (e: Exception) {
            refreshTokenIfNeeded()
            getKategoriCollection(uid).document(id).delete().await()
        }
    }
}
