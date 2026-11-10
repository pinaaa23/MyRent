package com.myrent.app.data.settings

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
        "telp_toko",
        "total", "dp", "sisa", "invoice", "nota", "catatan"
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
        telpToko: String = ""
    ): String {
        if (template.isEmpty()) return ""

        val namaVal = nama
        val tglKembaliVal = tanggal
        val tglSewaVal = tanggalSewa.ifEmpty { tglKembaliVal }

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
            .replace("{total}", total)
            .replace("{dp}", dp)
            .replace("{sisa}", sisa)
            .replace("{invoice}", invoice)
            .replace("{nota}", invoice)
            .replace("{catatan}", catatan.ifEmpty { "-" })

        if (telpToko.isBlank()) {
            result = result.replace("Telp/WA: ", "").replace("WA: ", "")
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
        ------------------------------------
        *No. Nota*    : {invoice}
        *Pelanggan*   : {nama_penyewa}

        *RINCIAN SEWA*:
        • Barang      : {barang}
        • Periode     : {tanggal_kembali}
        • Total Biaya : {total}
        • Uang Muka   : {dp}
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
