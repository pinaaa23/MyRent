package com.myrent.app.data.settings

/**
 * Model data profil toko yang disimpan di Firestore ("toko/{uid}/pengaturan/profil")
 */
data class StoreProfileDoc(
    val nama: String = "",
    val noTelp: String = "",
    val alamat: String = "",
    val fotoBase64: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
