package com.myrent.app.data.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pengelola Profil Toko terisolasi per akun di path "toko/{uid}/pengaturan/profil"
 */
class StoreProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    private val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified == true

    private fun getProfilDocRef(uid: String = currentUid) =
        firestore.collection("toko").document(uid.ifEmpty { "global" }).collection("pengaturan").document("profil")

    /**
     * Aliran data realtime StoreProfileDoc via Flow dari toko/{uid}/pengaturan/profil
     */
    fun getStoreProfileFlow(): Flow<StoreProfileDoc> = callbackFlow {
        val uid = currentUid
        if (uid.isEmpty() || !isEmailVerified) {
            trySend(StoreProfileDoc())
            awaitClose { }
            return@callbackFlow
        }

        val listener = getProfilDocRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(StoreProfileDoc())
                return@addSnapshotListener
            }

            val doc = StoreProfileDoc(
                nama = snapshot.getString("nama") ?: "",
                noTelp = snapshot.getString("noTelp") ?: "",
                alamat = snapshot.getString("alamat") ?: "",
                fotoBase64 = snapshot.getString("fotoBase64") ?: "",
                updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
            )

            trySend(doc)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Membaca StoreProfileDoc sekali dari toko/{uid}/pengaturan/profil
     */
    suspend fun getStoreProfile(): Result<StoreProfileDoc> = runCatching {
        val uid = currentUid
        if (uid.isEmpty() || !isEmailVerified) return Result.success(StoreProfileDoc())

        val snapshot = getProfilDocRef(uid).get().await()
        if (!snapshot.exists()) return Result.success(StoreProfileDoc())

        StoreProfileDoc(
            nama = snapshot.getString("nama") ?: "",
            noTelp = snapshot.getString("noTelp") ?: "",
            alamat = snapshot.getString("alamat") ?: "",
            fotoBase64 = snapshot.getString("fotoBase64") ?: "",
            updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
        )
    }

    /**
     * Menyimpan atau memperbarui profil toko di toko/{uid}/pengaturan/profil
     */
    suspend fun saveStoreProfile(profile: StoreProfileDoc): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }
        require(profile.nama.isNotBlank()) { "Nama toko wajib diisi." }

        val map = mapOf(
            "nama" to profile.nama.trim(),
            "noTelp" to profile.noTelp.trim(),
            "alamat" to profile.alamat.trim(),
            "fotoBase64" to profile.fotoBase64,
            "updatedAt" to System.currentTimeMillis()
        )
        getProfilDocRef(uid).set(map, SetOptions.merge()).await()
    }
}
