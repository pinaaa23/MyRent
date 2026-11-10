package com.myrent.app.data.items

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pengelola koleksi "items" terisolasi per akun toko (toko/{uid}/items)
 */
class ItemRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    private val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified == true

    private fun getItemCollection(uid: String = currentUid) =
        firestore.collection("toko").document(uid.ifEmpty { "global" }).collection("items")

    /**
     * Mendapatkan semua daftar items milik akun terisolasi toko/{uid}/items secara realtime via Flow.
     */
    fun getItemsFlow(): Flow<List<Item>> = callbackFlow {
        val uid = currentUid
        if (uid.isEmpty() || !isEmailVerified) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val userItemsRef = getItemCollection(uid)

        val listener = userItemsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.map { doc ->
                val stok = (doc.getLong("stok") ?: 1L).toInt()
                val stokTersedia = (doc.getLong("stokTersedia") ?: stok.toLong()).toInt()
                Item(
                    id = doc.id,
                    cabangId = doc.getString("cabangId") ?: "",
                    nama = doc.getString("nama") ?: "",
                    kategori = doc.getString("kategori") ?: "",
                    ukuran = doc.getString("ukuran") ?: "",
                    hargaSewa = doc.getLong("hargaSewa") ?: 0L,
                    stok = stok,
                    stokTersedia = stokTersedia,
                    fotoBase64 = doc.getString("fotoBase64") ?: "",
                    status = doc.getString("status") ?: "tersedia",
                    catatan = doc.getString("catatan") ?: "",
                    dibuatPada = doc.getLong("dibuatPada") ?: 0L
                )
            }?.sortedByDescending { it.dibuatPada } ?: emptyList()

            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun tambahItem(item: Item): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        val map = mapOf(
            "cabangId" to item.cabangId,
            "nama" to item.nama,
            "kategori" to item.kategori,
            "ukuran" to item.ukuran,
            "hargaSewa" to item.hargaSewa,
            "stok" to item.stok,
            "stokTersedia" to item.stok,
            "fotoBase64" to item.fotoBase64,
            "status" to item.status,
            "catatan" to item.catatan,
            "dibuatPada" to item.dibuatPada
        )
        getItemCollection(uid).add(map).await()
    }

    suspend fun updateItem(item: Item): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        val map = mapOf(
            "nama" to item.nama,
            "kategori" to item.kategori,
            "ukuran" to item.ukuran,
            "hargaSewa" to item.hargaSewa,
            "stok" to item.stok,
            "stokTersedia" to item.stokTersedia,
            "fotoBase64" to item.fotoBase64,
            "status" to item.status,
            "catatan" to item.catatan
        )
        getItemCollection(uid).document(item.id).update(map).await()
    }

    suspend fun hapusItem(id: String): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        getItemCollection(uid).document(id).delete().await()
    }
}
