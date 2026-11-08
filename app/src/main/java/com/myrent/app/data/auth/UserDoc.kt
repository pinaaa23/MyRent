package com.myrent.app.data.auth

/**
 * Model data untuk pengguna yang disimpan di Firestore ("users/{uid}")
 */
data class UserDoc(
    val uid: String = "",
    val nama: String = "",
    val email: String = "",
    val cabangId: String = "",
    val dibuatPada: Long = System.currentTimeMillis()
)
