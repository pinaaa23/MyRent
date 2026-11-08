package com.myrent.app.data.rentals

/**
 * Model data Transaksi Sewa ("rentals")
 */
data class Rental(
    val id: String = "",
    val cabangId: String = "",
    val itemId: String = "",
    val namaBarang: String = "",
    val customerId: String = "",
    val namaPenyewa: String = "",
    val noWa: String = "",
    val tanggalMulai: Long = System.currentTimeMillis(),
    val tanggalKembali: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000),
    val lamaHari: Int = 1,
    val jumlah: Int = 1, // Jumlah unit barang yang disewa dalam transaksi ini
    val hargaTotal: Long = 0L,
    val dp: Long = 0L,
    val sisaBayar: Long = 0L,
    val statusPembayaran: String = "lunas", // "lunas" atau "belum_lunas"
    val status: String = "berjalan", // "berjalan", "selesai", "terlambat"
    val sudahFollowUpH1: Boolean = false,
    val sudahFollowUpH: Boolean = false,
    val dibuatOleh: String = "",
    val dibuatPada: Long = System.currentTimeMillis(),
    val dikembalikanPada: Long? = null,
    val catatan: String = "",
    val invoiceId: String = "",           // ID Nota unik cth: INV-20261108-214934
    val metodePembayaran: String = "Tunai", // "Tunai" atau "QRIS"
    val pelunasanPada: Long? = null,      // Timestamp saat pelunasan dilakukan
    val jumlahPelunasan: Long = 0L        // Nominal pelunasan sisa bayar
)
