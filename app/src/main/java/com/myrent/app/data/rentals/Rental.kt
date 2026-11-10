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
    val jumlah: Int = 1,
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
    val metodePembayaran: String = "Tunai", // "Tunai", "QRIS", dll
    val pelunasanPada: Long? = null,      // Timestamp saat pelunasan dilakukan
    val jumlahPelunasan: Long = 0L,       // Nominal pelunasan sisa bayar
    val payments: List<PaymentRecord> = emptyList() // Riwayat riil catatan pembayaran DP & Pelunasan
) {
    /**
     * Mendapatkan daftar riwayat pembayaran lengkap.
     * Jika `payments` kosong (transaksi lama), otomatis membuat objek PaymentRecord virtual
     * dari nilai `dp` dan `jumlahPelunasan` / `pelunasanPada` tanpa merusak data lama.
     * Koreksi Data Lama: Jika dp == hargaTotal DAN jumlahPelunasan > 0, DP virtual = hargaTotal - jumlahPelunasan.
     */
    fun getAllPaymentRecords(): List<PaymentRecord> {
        if (payments.isNotEmpty()) return payments

        val synthesized = mutableListOf<PaymentRecord>()
        val metodeStr = metodePembayaran.trim().ifEmpty { "Tidak diketahui" }
        val jenisMetodeStr = mapJenisMetode(metodePembayaran)

        val realVirtualDp = if (dp == hargaTotal && jumlahPelunasan > 0L && hargaTotal > jumlahPelunasan) {
            hargaTotal - jumlahPelunasan
        } else {
            dp
        }

        if (realVirtualDp > 0L) {
            synthesized.add(
                PaymentRecord(
                    id = "dp_$id",
                    rentalId = id,
                    jumlah = realVirtualDp,
                    jenis = "DP",
                    metode = metodeStr,
                    jenisMetode = jenisMetodeStr,
                    tanggal = dibuatPada
                )
            )
        }

        if (jumlahPelunasan > 0L && pelunasanPada != null) {
            synthesized.add(
                PaymentRecord(
                    id = "pelunasan_$id",
                    rentalId = id,
                    jumlah = jumlahPelunasan,
                    jenis = "Pelunasan",
                    metode = metodeStr,
                    jenisMetode = jenisMetodeStr,
                    tanggal = pelunasanPada
                )
            )
        }

        return synthesized
    }
}
