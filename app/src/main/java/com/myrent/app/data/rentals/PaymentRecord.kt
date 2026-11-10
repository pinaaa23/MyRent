package com.myrent.app.data.rentals

/**
 * Model data Catatan Riwayat Pembayaran (DP & Pelunasan) per Transaksi Sewa
 */
data class PaymentRecord(
    val id: String = "",
    val rentalId: String = "",
    val jumlah: Long = 0L,
    val jenis: String = "DP", // "DP" atau "Pelunasan"
    val metode: String = "Tunai", // "Tunai", "QRIS", "Transfer", "Kartu", "Tidak diketahui", dll.
    val jenisMetode: String = "TUNAI", // "TUNAI", "NON_TUNAI", atau "UNSPECIFIED"
    val tanggal: Long = System.currentTimeMillis()
)

/**
 * Helper untuk mengelompokkan metode pembayaran ke jenis "TUNAI", "NON_TUNAI", atau "UNSPECIFIED"
 */
fun mapJenisMetode(metodeRaw: String): String {
    val norm = metodeRaw.trim().uppercase()
    return when {
        norm == "TUNAI" || norm == "CASH" -> "TUNAI"
        norm.isEmpty() || norm == "TIDAK DIKETAHUI" || norm == "UNSPECIFIED" -> "UNSPECIFIED"
        else -> "NON_TUNAI"
    }
}
