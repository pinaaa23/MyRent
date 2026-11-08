package com.myrent.app.data.items

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pengelola koleksi "items" di Firestore
 */
class ItemRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Mendapatkan daftar items berdasarkan cabangId secara realtime via Flow.
     * Diurutkan di sisi Kotlin / Aplikasi (tanpa orderBy di query Firestore) agar tidak membutuhkan index komposit.
     */
    fun getItemsFlow(cabangId: String): Flow<List<Item>> = callbackFlow {
        if (cabangId.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("items")
            .whereEqualTo("cabangId", cabangId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
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
        val map = mapOf(
            "cabangId" to item.cabangId,
            "nama" to item.nama,
            "kategori" to item.kategori,
            "ukuran" to item.ukuran,
            "hargaSewa" to item.hargaSewa,
            "stok" to item.stok,
            "stokTersedia" to item.stok, // Stok awal tersedia selalu sama dengan stok total saat pertama dibuat
            "fotoBase64" to item.fotoBase64,
            "status" to item.status,
            "catatan" to item.catatan,
            "dibuatPada" to item.dibuatPada
        )
        firestore.collection("items").add(map).await()
    }

    suspend fun updateItem(item: Item): Result<Unit> = runCatching {
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
        firestore.collection("items").document(item.id).update(map).await()
    }

    suspend fun hapusItem(id: String): Result<Unit> = runCatching {
        firestore.collection("items").document(id).delete().await()
    }
}
