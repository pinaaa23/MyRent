package com.myrent.app.data.rentals

/**
 * Model data Transaksi Sewa ("rentals") dengan Dukungan Multi-Item & Media Sosial Konsumen
 */
data class Rental(
    val id: String = "",
    val cabangId: String = "",
    val itemId: String = "",
    val namaBarang: String = "",
    val customerId: String = "",
    val namaPenyewa: String = "",
    val noWa: String = "",
    val medsosPenyewa: String = "",      // Akun Media Sosial Konsumen (IG / TikTok / FB)
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
    val items: List<RentalItem> = emptyList(), // Daftar rincian barang yang disewa (Multi-Item)
    val payments: List<PaymentRecord> = emptyList() // Riwayat riil catatan pembayaran DP & Pelunasan
) {
    /**
     * Mendapatkan daftar rincian barang yang disewa.
     * Jika `items` tidak kosong, mengembalikan `items`.
     * Jika `items` kosong (transaksi lama 1 barang), otomatis membuat 1 objek RentalItem virtual.
     */
    fun getItemsList(): List<RentalItem> {
        if (items.isNotEmpty()) return items

        val unitPrice = if (jumlah > 0 && lamaHari > 0) hargaTotal / (jumlah * lamaHari.coerceAtLeast(1)) else hargaTotal
        return listOf(
            RentalItem(
                itemId = itemId,
                nama = namaBarang.ifEmpty { "Barang Sewa" },
                kategori = "",
                hargaSatuan = unitPrice,
                jumlah = jumlah.coerceAtLeast(1),
                fotoBase64 = ""
            )
        )
    }

    /**
     * Teks singkat nama barang untuk kartu sewa, riwayat penyewa, dan daftar follow up
     * Contoh 1 barang: "Jas Hitam"
     * Contoh > 1 barang: "Jas Hitam +1 lainnya"
     */
    fun getNamaDisplay(): String {
        val list = getItemsList()
        if (list.isEmpty()) return namaBarang.ifEmpty { "Barang Sewa" }
        return if (list.size == 1) {
            list[0].nama
        } else {
            "${list[0].nama} +${list.size - 1} lainnya"
        }
    }

    /**
     * Teks rincian barang untuk template pesan WhatsApp
     * Contoh: "Jas Hitam (1 unit), Sepatu (2 unit)"
     */
    fun getSummaryBarangWA(): String {
        val list = getItemsList()
        if (list.isEmpty()) return "${namaBarang} (${jumlah} unit)"
        return list.joinToString(", ") { "${it.nama} (${it.jumlah} unit)" }
    }

    /**
     * Mendapatkan total unit barang yang disewa dalam transaksi ini
     */
    fun getTotalUnitCount(): Int {
        val list = getItemsList()
        if (list.isEmpty()) return jumlah.coerceAtLeast(1)
        return list.sumOf { it.jumlah }
    }

    /**
     * Mendapatkan daftar riwayat pembayaran lengkap.
     * Jika `payments` kosong (transaksi lama), otomatis membuat objek PaymentRecord virtual.
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
