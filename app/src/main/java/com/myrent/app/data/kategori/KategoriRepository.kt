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

    /**
     * Mendapatkan daftar kategori secara realtime via Flow milik akun toko (toko/{uid}/kategori).
     * Membaca murni dari toko/{uid}/kategori tanpa membuat kategori ganda otomatis saat kosong.
     */
    fun getKategoriFlow(): Flow<List<Kategori>> = callbackFlow {
        val uid = currentUid
        if (uid.isEmpty() || !isEmailVerified) {
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
     * Menambah kategori baru dengan pencegahan nama ganda (tidak membedakan huruf besar/kecil)
     */
    suspend fun tambahKategori(nama: String): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }
        val trimmed = nama.trim()
        require(trimmed.isNotEmpty()) { "Nama kategori tidak boleh kosong." }

        val userKatRef = getKategoriCollection(uid)
        val existingDocs = userKatRef.get().await()
        val isDuplicate = existingDocs.documents.any {
            (it.getString("nama") ?: "").equals(trimmed, ignoreCase = true)
        }

        if (isDuplicate) {
            error("Kategori \"$trimmed\" sudah ada.")
        }

        userKatRef.add(mapOf("nama" to trimmed)).await()
    }

    /**
     * Mengedit nama kategori dengan pencegahan nama ganda
     */
    suspend fun editKategori(id: String, namaBaru: String): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }
        val trimmed = namaBaru.trim()
        require(trimmed.isNotEmpty()) { "Nama kategori tidak boleh kosong." }

        val userKatRef = getKategoriCollection(uid)
        val existingDocs = userKatRef.get().await()
        val isDuplicate = existingDocs.documents.any {
            it.id != id && (it.getString("nama") ?: "").equals(trimmed, ignoreCase = true)
        }

        if (isDuplicate) {
            error("Kategori \"$trimmed\" sudah ada.")
        }

        userKatRef.document(id).update("nama", trimmed).await()
    }

    /**
     * Menghapus dokumen kategori dari toko/{uid}/kategori/{id}
     */
    suspend fun hapusKategori(id: String): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }
        getKategoriCollection(uid).document(id).delete().await()
    }
}
