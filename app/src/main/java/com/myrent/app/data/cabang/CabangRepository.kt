package com.myrent.app.data.cabang

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pengelola koleksi "cabang" di Firestore
 */
class CabangRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Membaca aliran data daftar cabang secara realtime
     */
    fun getDaftarCabangFlow(): Flow<List<Cabang>> = callbackFlow {
        val listener = firestore.collection("cabang")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    Cabang(
                        id = doc.id,
                        nama = doc.getString("nama") ?: "",
                        alamat = doc.getString("alamat") ?: "",
                        noWa = doc.getString("noWa") ?: ""
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getDaftarCabang(): Result<List<Cabang>> = runCatching {
        val snapshot = firestore.collection("cabang").get().await()
        snapshot.documents.map { doc ->
            Cabang(
                id = doc.id,
                nama = doc.getString("nama") ?: "",
                alamat = doc.getString("alamat") ?: "",
                noWa = doc.getString("noWa") ?: ""
            )
        }
    }

    suspend fun getCabangById(id: String): Result<Cabang> = runCatching {
        if (id.isEmpty()) return Result.success(Cabang(nama = "Cabang Utama"))
        val doc = firestore.collection("cabang").document(id).get().await()
        Cabang(
            id = doc.id,
            nama = doc.getString("nama") ?: "",
            alamat = doc.getString("alamat") ?: "",
            noWa = doc.getString("noWa") ?: ""
        )
    }

    suspend fun tambahCabang(cabang: Cabang): Result<Unit> = runCatching {
        val map = mapOf(
            "nama" to cabang.nama.trim(),
            "alamat" to cabang.alamat.trim(),
            "noWa" to cabang.noWa.trim()
        )
        firestore.collection("cabang").add(map).await()
    }

    suspend fun updateCabang(cabang: Cabang): Result<Unit> = runCatching {
        val map = mapOf(
            "nama" to cabang.nama.trim(),
            "alamat" to cabang.alamat.trim(),
            "noWa" to cabang.noWa.trim()
        )
        firestore.collection("cabang").document(cabang.id).update(map).await()
    }
}
