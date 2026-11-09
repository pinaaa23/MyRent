package com.myrent.app.data.auth

/**
 * Model data profil pengguna & toko yang disimpan di Firestore ("users/{uid}")
 */
data class UserDoc(
    val uid: String = "",
    val nama: String = "",            // Nama Pemilik
    val namaToko: String = "",        // Nama Toko / Cabang
    val noWaToko: String = "",        // Nomor WA Toko
    val alamatToko: String = "",      // Alamat Toko
    val fotoProfilBase64: String = "",// Base64 Logo / Foto Toko
    val email: String = "",
    val migrasiNamaToko: Boolean = false, // Flag migrasi 1x namaToko Kusuma Rent
    val dibuatPada: Long = System.currentTimeMillis()
)
