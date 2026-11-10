package com.myrent.app.data.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pengelola Kustom Nota & Template Follow-up WA di path "toko/{uid}/pengaturan/nota"
 */
class NotaConfigRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    private val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified == true

    private fun getNotaDocRef(uid: String = currentUid) =
        firestore.collection("toko").document(uid.ifEmpty { "global" }).collection("pengaturan").document("nota")

    /**
     * Aliran data realtime NotaConfigDoc via Flow dari toko/{uid}/pengaturan/nota
     */
    fun getNotaConfigFlow(): Flow<NotaConfigDoc> = callbackFlow {
        val uid = currentUid
        if (uid.isEmpty() || !isEmailVerified) {
            trySend(NotaConfigDoc())
            awaitClose { }
            return@callbackFlow
        }

        val listener = getNotaDocRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(NotaConfigDoc())
                return@addSnapshotListener
            }

            val doc = NotaConfigDoc(
                judulNota = snapshot.getString("judulNota") ?: "NOTA",
                kalimatPembuka = snapshot.getString("kalimatPembuka") ?: snapshot.getString("notaHeader") ?: "",
                catatanBawah = snapshot.getString("catatanBawah") ?: snapshot.getString("notaFooter") ?: "",
                flagTampilAlamat = snapshot.getBoolean("flagTampilAlamat") ?: true,
                flagTampilTelp = snapshot.getBoolean("flagTampilTelp") ?: true,
                templateFollowUpH1 = snapshot.getString("templateFollowUpH1") ?: snapshot.getString("templatePesanH1") ?: "",
                templateFollowUpHariH = snapshot.getString("templateFollowUpHariH") ?: snapshot.getString("templatePesanH") ?: "",
                templateFollowUpTelat = snapshot.getString("templateFollowUpTelat") ?: snapshot.getString("templatePesanTelat") ?: "",
                updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
            )

            trySend(doc)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Membaca NotaConfigDoc sekali
     */
    suspend fun getNotaConfig(): Result<NotaConfigDoc> = runCatching {
        val uid = currentUid
        if (uid.isEmpty() || !isEmailVerified) return Result.success(NotaConfigDoc())

        val snapshot = getNotaDocRef(uid).get().await()
        if (!snapshot.exists()) return Result.success(NotaConfigDoc())

        NotaConfigDoc(
            judulNota = snapshot.getString("judulNota") ?: "NOTA",
            kalimatPembuka = snapshot.getString("kalimatPembuka") ?: snapshot.getString("notaHeader") ?: "",
            catatanBawah = snapshot.getString("catatanBawah") ?: snapshot.getString("notaFooter") ?: "",
            flagTampilAlamat = snapshot.getBoolean("flagTampilAlamat") ?: true,
            flagTampilTelp = snapshot.getBoolean("flagTampilTelp") ?: true,
            templateFollowUpH1 = snapshot.getString("templateFollowUpH1") ?: snapshot.getString("templatePesanH1") ?: "",
            templateFollowUpHariH = snapshot.getString("templateFollowUpHariH") ?: snapshot.getString("templatePesanH") ?: "",
            templateFollowUpTelat = snapshot.getString("templateFollowUpTelat") ?: snapshot.getString("templatePesanTelat") ?: "",
            updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
        )
    }

    /**
     * Menyimpan konfigurasi Nota (Judul, Pembuka, Catatan Bawah, Switch Alamat, Switch WA)
     */
    suspend fun saveNotaConfig(
        judulNota: String,
        kalimatPembuka: String,
        catatanBawah: String,
        flagTampilAlamat: Boolean,
        flagTampilTelp: Boolean
    ): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        val map = mapOf(
            "judulNota" to judulNota.trim().ifEmpty { "NOTA" },
            "kalimatPembuka" to kalimatPembuka.trim(),
            "notaHeader" to kalimatPembuka.trim(),
            "catatanBawah" to catatanBawah.trim(),
            "notaFooter" to catatanBawah.trim(),
            "flagTampilAlamat" to flagTampilAlamat,
            "flagTampilTelp" to flagTampilTelp,
            "updatedAt" to System.currentTimeMillis()
        )
        getNotaDocRef(uid).set(map, SetOptions.merge()).await()
    }

    /**
     * Menyimpan template pesan WA (H-1, Hari H, Telat)
     */
    suspend fun saveWaTemplates(
        templateH1: String,
        templateHariH: String,
        templateTelat: String
    ): Result<Unit> = runCatching {
        val uid = currentUid
        require(uid.isNotEmpty() && isEmailVerified) { "Pengguna belum login atau email belum diverifikasi" }

        val map = mapOf(
            "templateFollowUpH1" to templateH1.trim(),
            "templatePesanH1" to templateH1.trim(),
            "templateFollowUpHariH" to templateHariH.trim(),
            "templatePesanH" to templateHariH.trim(),
            "templateFollowUpTelat" to templateTelat.trim(),
            "templatePesanTelat" to templateTelat.trim(),
            "updatedAt" to System.currentTimeMillis()
        )
        getNotaDocRef(uid).set(map, SetOptions.merge()).await()
    }
}
