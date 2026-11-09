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

    private fun getKategoriCollection(uid: String = currentUid) =
        firestore.collection("toko").document(uid.ifEmpty { "global" }).collection("kategori")

    /**
     * Mendapatkan daftar kategori secara realtime via Flow milik akun toko.
     * Jika koleksi toko/{uid}/kategori masih kosong, otomatis menambahkan kategori "Jas".
     */
    fun getKategoriFlow(): Flow<List<Kategori>> = callbackFlow {
        val uid = currentUid
        if (uid.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val userKatRef = getKategoriCollection(uid)

        // Cek migrasi data lama jika brankas toko/{uid}/kategori akun ini masih kosong
        userKatRef.limit(1).get().addOnSuccessListener { userSnap ->
            if (userSnap.isEmpty) {
                firestore.collection("kategori").get().addOnSuccessListener { legacySnap ->
                    if (!legacySnap.isEmpty) {
                        legacySnap.documents.forEach { doc ->
                            userKatRef.document(doc.id).set(doc.data ?: emptyMap<String, Any>())
                        }
                    } else {
                        userKatRef.add(mapOf("nama" to "Jas"))
                    }
                }
            }
        }

        val listener = userKatRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.map { doc ->
                Kategori(id = doc.id, nama = doc.getString("nama") ?: "")
            } ?: emptyList()

            if (list.isEmpty()) {
                userKatRef.add(mapOf("nama" to "Jas"))
            }

            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun tambahKategori(nama: String): Result<Unit> = runCatching {
        getKategoriCollection().add(mapOf("nama" to nama.trim())).await()
    }

    suspend fun editKategori(id: String, namaBaru: String): Result<Unit> = runCatching {
        getKategoriCollection().document(id).update("nama", namaBaru.trim()).await()
    }

    suspend fun hapusKategori(id: String): Result<Unit> = runCatching {
        getKategoriCollection().document(id).delete().await()
    }
}
