package com.myrent.app.data.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pengelola Kustom Nota & Template Pesan WA terisolasi per toko (toko/{uid}/settings/config)
 */
class StoreConfigRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    private val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified == true

    private fun getConfigDocRef(uid: String = currentUid) =
        firestore.collection("toko").document(uid.ifEmpty { "global" }).collection("settings").document("config")

    /**
     * Aliran data realtime StoreConfigDoc via Flow
     */
    fun getStoreConfigFlow(): Flow<StoreConfigDoc> = callbackFlow {
        val uid = currentUid
        if (uid.isEmpty() || !isEmailVerified) {
            trySend(StoreConfigDoc())
            awaitClose { }
            return@callbackFlow
        }

        val listener = getConfigDocRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(StoreConfigDoc())
                return@addSnapshotListener
            }

            val doc = StoreConfigDoc(
                notaHeader = snapshot.getString("notaHeader") ?: "",
                notaFooter = snapshot.getString("notaFooter") ?: "",
                templatePesanNota = snapshot.getString("templatePesanNota") ?: "",
                templatePesanH1 = snapshot.getString("templatePesanH1") ?: "",
                templatePesanH = snapshot.getString("templatePesanH") ?: "",
                templatePesanTelat = snapshot.getString("templatePesanTelat") ?: ""
            )

            trySend(doc)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Membaca StoreConfigDoc sekali
     */
    suspend fun getStoreConfig(): Result<StoreConfigDoc> = runCatching {
        val uid = currentUid
        if (uid.isEmpty() || !isEmailVerified) return Result.success(StoreConfigDoc())

        val snapshot = getConfigDocRef(uid).get().await()
        if (!snapshot.exists()) return Result.success(StoreConfigDoc())

        StoreConfigDoc(
            notaHeader = snapshot.getString("notaHeader") ?: "",
            notaFooter = snapshot.getString("notaFooter") ?: "",
            templatePesanNota = snapshot.getString("templatePesanNota") ?: "",
            templatePesanH1 = snapshot.getString("templatePesanH1") ?: "",
            templatePesanH = snapshot.getString("templatePesanH") ?: "",
            templatePesanTelat = snapshot.getString("templatePesanTelat") ?: ""
        )
    }

    /**
     * Menyimpan atau memperbarui konfigurasi kustom nota
     */
    suspend fun saveNotaConfig(header: String, footer: String): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        val map = mapOf(
            "notaHeader" to header.trim(),
            "notaFooter" to footer.trim()
        )
        getConfigDocRef(uid).set(map, SetOptions.merge()).await()
    }

    /**
     * Menyimpan atau memperbarui template pesan WA
     */
    suspend fun saveWaTemplates(
        templateNota: String,
        templateH1: String,
        templateH: String,
        templateTelat: String = ""
    ): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }
        require(templateNota.isNotBlank() && templateH1.isNotBlank() && templateH.isNotBlank()) {
            "Template pesan tidak boleh kosong."
        }

        val map = mapOf(
            "templatePesanNota" to templateNota.trim(),
            "templatePesanH1" to templateH1.trim(),
            "templatePesanH" to templateH.trim(),
            "templatePesanTelat" to templateTelat.trim()
        )
        getConfigDocRef(uid).set(map, SetOptions.merge()).await()
    }
}
