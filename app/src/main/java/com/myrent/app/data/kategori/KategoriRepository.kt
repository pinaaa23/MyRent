package com.myrent.app.data.kategori

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pengelola koleksi "kategori" di Firestore
 */
class KategoriRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Mendapatkan daftar kategori secara realtime via Flow.
     * Jika koleksi kategori di Firestore masih kosong, otomatis menambahkan kategori "Jas".
     */
    fun getKategoriFlow(): Flow<List<Kategori>> = callbackFlow {
        val listener = firestore.collection("kategori")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    Kategori(id = doc.id, nama = doc.getString("nama") ?: "")
                } ?: emptyList()

                // Jika koleksi kosong saat pertama kali dibuka, otomatis buat kategori "Jas"
                if (list.isEmpty()) {
                    firestore.collection("kategori").add(mapOf("nama" to "Jas"))
                }

                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun tambahKategori(nama: String): Result<Unit> = runCatching {
        firestore.collection("kategori").add(mapOf("nama" to nama.trim())).await()
    }

    suspend fun editKategori(id: String, namaBaru: String): Result<Unit> = runCatching {
        firestore.collection("kategori").document(id).update("nama", namaBaru.trim()).await()
    }

    suspend fun hapusKategori(id: String): Result<Unit> = runCatching {
        firestore.collection("kategori").document(id).delete().await()
    }
}
