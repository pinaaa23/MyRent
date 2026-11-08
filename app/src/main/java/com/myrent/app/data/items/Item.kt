package com.myrent.app.data.items

/**
 * Model data untuk Barang Sewa ("items")
 */
data class Item(
    val id: String = "",
    val cabangId: String = "",
    val nama: String = "",
    val kategori: String = "",
    val ukuran: String = "",
    val hargaSewa: Long = 0L,
    val stok: Int = 1,          // Total stok barang
    val stokTersedia: Int = 1,  // Stok yang saat ini tersedia
    val fotoBase64: String = "",
    val status: String = "tersedia", // "tersedia", "disewa", "perbaikan"
    val catatan: String = "",
    val dibuatPada: Long = System.currentTimeMillis()
)
