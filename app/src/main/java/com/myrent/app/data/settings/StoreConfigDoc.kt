package com.myrent.app.data.settings

import com.myrent.app.ui.screens.formatRupiah

/**
 * Model data konfigurasi kustom nota & template pesan WA toko
 * Disimpan di Firestore "toko/{uid}/settings/config"
 */
data class StoreConfigDoc(
    val notaHeader: String = "",
    val notaFooter: String = "",
    val templatePesanNota: String = "",
    val templatePesanH1: String = "",
    val templatePesanH: String = "",
    val templatePesanTelat: String = ""
)

/**
 * Helper untuk penggantian variabel template pesan WhatsApp & Nota
 */
object TemplateReplacer {

    val KNOWN_PLACEHOLDERS = setOf(
        "nama_penyewa", "nama",
        "barang",
        "tanggal_sewa",
        "tanggal_kembali", "tanggal",
        "hari_telat", "telat",
        "nama_toko", "toko",
        "telp_toko", "telp",
        "alamat_toko", "alamat",
        "total", "dp", "pelunasan", "sisa", "invoice", "nota", "catatan"
    )

    fun replaceVariables(
        template: String,
        nama: String = "",
        barang: String = "",
        tanggal: String = "",
        total: String = "",
        dp: String = "",
        sisa: String = "",
        invoice: String = "",
        toko: String = "",
        catatan: String = "-",
        telat: String = "0",
        tanggalSewa: String = "",
        telpToko: String = "",
        alamatToko: String = "",
        jumlahPelunasan: Long = 0L,
        pelunasanMetode: String = "",
        pelunasanTanggalStr: String = ""
    ): String {
        if (template.isEmpty()) return ""

        val namaVal = nama
        val tglKembaliVal = tanggal
        val tglSewaVal = tanggalSewa.ifEmpty { tglKembaliVal }

        val pelunasanLine = if (jumlahPelunasan > 0L) {
            val met = pelunasanMetode.ifEmpty { "Tidak diketahui" }.lowercase()
            val tglStr = if (pelunasanTanggalStr.isNotEmpty()) " ($pelunasanTanggalStr)" else ""
            "Pelunasan ($met)$tglStr: ${formatRupiah(jumlahPelunasan)}"
        } else ""

        var result = template
            .replace("{nama_penyewa}", namaVal)
            .replace("{nama}", namaVal)
            .replace("{barang}", barang)
            .replace("{tanggal_sewa}", tglSewaVal)
            .replace("{tanggal_kembali}", tglKembaliVal)
            .replace("{tanggal}", tglKembaliVal)
            .replace("{hari_telat}", telat)
            .replace("{telat}", telat)
            .replace("{nama_toko}", toko)
            .replace("{toko}", toko)
            .replace("{telp_toko}", telpToko)
            .replace("{telp}", telpToko)
            .replace("{alamat_toko}", alamatToko)
            .replace("{alamat}", alamatToko)
            .replace("{total}", total)
            .replace("{dp}", dp)
            .replace("{pelunasan}", pelunasanLine)
            .replace("{sisa}", sisa)
            .replace("{invoice}", invoice)
            .replace("{nota}", invoice)
            .replace("{catatan}", catatan.ifEmpty { "-" })

        if (pelunasanLine.isEmpty()) {
            result = result.replace("• Pelunasan   : {pelunasan}\n", "")
                .replace("Pelunasan   : {pelunasan}\n", "")
                .replace("• {pelunasan}\n", "")
                .replace("{pelunasan}\n", "")
        }

        if (telpToko.isBlank()) {
            result = result.replace("Telp/WA: \n", "").replace("Telp/WA: ", "").replace("WA: ", "")
        }
        if (alamatToko.isBlank()) {
            result = result.replace("\n\n", "\n")
        }

        return result
    }

    /**
     * Memeriksa variabel yang tidak dikenal di dalam template
     */
    fun findUnknownVariables(template: String): List<String> {
        val regex = Regex("\\{([a-zA-Z0-9_]+)\\}")
        val matches = regex.findAll(template)
        return matches.map { it.groupValues[1] }
            .filter { it !in KNOWN_PLACEHOLDERS }
            .distinct()
            .toList()
    }
}

/**
 * Teks Bawaan (Default Fallback) untuk Template Pesan WhatsApp & Nota
 */
object DefaultStoreConfig {
    const val NOTA_JUDUL_DEFAULT = "NOTA"
    const val NOTA_HEADER_DEFAULT = "" // Default header harus kosong
    const val NOTA_FOOTER_DEFAULT = "* Mohon tunjukkan nota ini saat pengembalian barang.\nTerima kasih telah menyewa alat di MyRent."

    val TEMPLATE_NOTA_DEFAULT = """
        *NOTA TRANSAKSI SEWA - {nama_toko}*
        {alamat_toko}
        Telp/WA: {telp_toko}
        ------------------------------------
        *No. Nota*    : {invoice}
        *Pelanggan*   : {nama_penyewa}

        *RINCIAN SEWA*:
        • Barang      : {barang}
        • Periode     : {tanggal_kembali}
        • Total Biaya : {total}
        • Uang Muka   : {dp}
        • {pelunasan}
        • Sisa Bayar  : {sisa}
        • Jaminan/Note: {catatan}

        ------------------------------------
        *Catatan*: Mohon tunjukkan nota / pesan ini saat melakukan pengembalian barang.
        Terima kasih telah menyewa alat di MyRent!
    """.trimIndent()

    val TEMPLATE_H1_DEFAULT = "Halo {nama_penyewa}, kami dari toko {nama_toko} mengingatkan bahwa {barang} dijadwalkan dikembalikan besok, {tanggal_kembali}. Terima kasih."

    val TEMPLATE_H_DEFAULT = "Halo {nama_penyewa}, hari ini jadwal pengembalian {barang}. Mohon dikembalikan hari ini ya. Terima kasih."

    val TEMPLATE_TELAT_DEFAULT = "Halo {nama_penyewa}, kami dari toko {nama_toko} mengingatkan bahwa {barang} yang disewa telah melewati tanggal pengembalian ({tanggal_kembali}) selama {hari_telat} hari. Mohon segera mengembalikan barang tersebut. Terima kasih."
}
