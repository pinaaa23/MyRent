package com.myrent.app.data.cabang

/**
 * Model data untuk Cabang toko yang disimpan di Firestore ("cabang")
 */
data class Cabang(
    val id: String = "",
    val nama: String = "",
    val alamat: String = "",
    val noWa: String = ""
)
