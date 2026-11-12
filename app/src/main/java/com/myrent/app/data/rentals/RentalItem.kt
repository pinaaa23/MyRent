package com.myrent.app.data.rentals

/**
 * Model data Rincian Barang yang Disewa di dalam Satu Transaksi (Multi-Item Rental)
 */
data class RentalItem(
    val itemId: String = "",
    val nama: String = "",
    val kategori: String = "",
    val hargaSatuan: Long = 0L,
    val jumlah: Int = 1,
    val fotoBase64: String = ""
)
