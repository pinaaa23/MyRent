package com.myrent.app.data.settings

import com.myrent.app.data.rentals.Rental
import com.myrent.app.ui.screens.formatDate
import com.myrent.app.ui.screens.formatRupiah

/**
 * Pengelola Terpusat Template Pesan WhatsApp Follow-Up Pengingat Pengembalian & Nota Transaksi
 * Mendukung pembacaan Kustom Template dari Firestore (StoreConfigDoc)
 */
object WaTemplates {

    /**
     * Membentuk pesan pengingat pengembalian WA berdasarkan kondisi (H-1, Hari H, atau Terlambat)
     * Jika `customTemplate` tidak kosong, menggunakan kustom template editan pengguna dari Firestore.
     */
    fun buildFollowUpMessage(
        rental: Rental,
        type: String, // "H-1", "Hari H", atau "TELAT"
        overdueDays: Int,
        namaToko: String,
        jamBatas: String,
        customTemplate: String = ""
    ): String {
        val namaVal = rental.namaPenyewa.ifEmpty { "Pelanggan" }
        val tokoVal = namaToko.ifEmpty { "MyRent Store" }
        val barangVal = rental.getSummaryBarangWA()
        val tglKembaliStr = formatDate(rental.tanggalKembali)
        val jamBatasVal = jamBatas.ifEmpty { "18.30" }

        // Jika pengguna sudah mengedit kustom template di Pengaturan, gunakan kustom template tersebut!
        if (customTemplate.trim().isNotEmpty()) {
            return TemplateReplacer.replaceVariables(
                template = customTemplate,
                nama = namaVal,
                barang = barangVal,
                tanggal = tglKembaliStr,
                total = formatRupiah(rental.hargaTotal),
                dp = formatRupiah(rental.dp),
                sisa = formatRupiah(rental.sisaBayar),
                invoice = rental.invoiceId,
                toko = tokoVal,
                telat = overdueDays.toString(),
                catatan = rental.catatan
            ).replace("{jamBatas}", jamBatasVal)
        }

        // Default Template jika belum pernah diedit
        val body = when (type) {
            "H-1" -> {
                "Halo $namaVal, kami dari $tokoVal mengingatkan bahwa $barangVal dijadwalkan dikembalikan besok, $tglKembaliStr. Maksimal pengembalian pada pukul $jamBatasVal WIB."
            }
            "Hari H" -> {
                "Halo $namaVal, kami dari $tokoVal mengingatkan bahwa $barangVal dijadwalkan dikembalikan hari ini, $tglKembaliStr. Maksimal pengembalian pada pukul $jamBatasVal WIB."
            }
            else -> { // "TELAT"
                "Halo $namaVal, kami dari $tokoVal mengingatkan bahwa $barangVal seharusnya dikembalikan pada $tglKembaliStr (terlambat $overdueDays hari). Mohon segera dikembalikan."
            }
        }

        val sisaRow = if (rental.sisaBayar > 0L) {
            "\nSisa pembayaran: ${formatRupiah(rental.sisaBayar)}\n"
        } else "\n"

        return "$body$sisaRow\nTerima kasih."
    }

    /**
     * Membentuk pesan Nota Transaksi WA (Bukti Transaksi)
     * Jika `customTemplate` tidak kosong, menggunakan kustom template editan pengguna dari Firestore.
     */
    fun buildNotaMessage(
        rental: Rental,
        namaToko: String,
        noWaToko: String,
        jamBatas: String,
        customTemplate: String = ""
    ): String {
        val tokoUpper = namaToko.ifEmpty { "MYRENT STORE" }.uppercase()
        val tokoName = namaToko.ifEmpty { "MyRent Store" }
        val invoice = rental.invoiceId.ifEmpty { "INV-${rental.dibuatPada}" }
        val tglMulaiStr = formatDate(rental.tanggalMulai)
        val tglKembaliStr = formatDate(rental.tanggalKembali)
        val waktuStr = formatDate(rental.dibuatPada)
        val jamBatasVal = jamBatas.ifEmpty { "18.30" }

        val itemsList = rental.getItemsList()
        val barangLines = if (itemsList.size <= 1) {
            rental.getSummaryBarangWA()
        } else {
            val sb = StringBuilder("\n")
            itemsList.forEach { item ->
                sb.append("   - ${item.nama} (${item.jumlah} unit)\n")
            }
            sb.toString().trimEnd()
        }

        val dpStr = formatRupiah(rental.dp)
        val pelunasanRow = if (rental.jumlahPelunasan > 0L) {
            "• Pelunasan   : ${formatRupiah(rental.jumlahPelunasan)}\n"
        } else ""

        val sisaStr = formatRupiah(rental.sisaBayar)
        val statusStr = if (rental.sisaBayar == 0L) "LUNAS" else "BELUM LUNAS"

        val metodeVal = if (rental.dp == 0L && rental.jumlahPelunasan == 0L) "-" else rental.metodePembayaran.ifEmpty { "-" }
        val catatanVal = rental.catatan.ifEmpty { "-" }

        // Jika pengguna sudah mengedit kustom template nota di Pengaturan, gunakan kustom template tersebut!
        if (customTemplate.trim().isNotEmpty()) {
            return TemplateReplacer.replaceVariables(
                template = customTemplate,
                nama = rental.namaPenyewa,
                barang = barangLines,
                tanggal = "$tglMulaiStr s/d $tglKembaliStr (${rental.lamaHari} Hari)",
                total = formatRupiah(rental.hargaTotal),
                dp = dpStr,
                sisa = sisaStr,
                invoice = invoice,
                toko = tokoName,
                catatan = catatanVal,
                telpToko = noWaToko,
                jumlahPelunasan = rental.jumlahPelunasan,
                pelunasanMetode = rental.metodePembayaran,
                pelunasanTanggalStr = if (rental.pelunasanPada != null) formatDate(rental.pelunasanPada) else ""
            ).replace("{jamBatas}", jamBatasVal)
                .replace("{NAMA_TOKO}", tokoUpper)
        }

        // Default Template jika belum pernah diedit
        return """
*NOTA TRANSAKSI SEWA - $tokoUpper*
------------------------------------
*No. Nota*    : $invoice
*Waktu*       : $waktuStr
*Pelanggan*   : ${rental.namaPenyewa} (${rental.noWa})

*RINCIAN SEWA*:
• Barang      : $barangLines
• Periode     : $tglMulaiStr s/d $tglKembaliStr (${rental.lamaHari} Hari)
• Total Biaya : ${formatRupiah(rental.hargaTotal)}
• Uang Muka   : $dpStr
${pelunasanRow}• Sisa Bayar  : $sisaStr
• Status      : $statusStr
• Metode      : $metodeVal
• Jaminan/Note: $catatanVal

------------------------------------
*Catatan*: Mohon tunjukkan nota ini saat pengambilan dan pengembalian barang. Maksimal pengembalian pukul $jamBatasVal WIB.

Terima kasih telah menyewa di $tokoName!
""".trimIndent()
    }
}
