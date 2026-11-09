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
    val templatePesanH: String = ""
)

/**
 * Helper untuk penggantian variabel template pesan WhatsApp
 */
object TemplateReplacer {
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
        catatan: String = "-"
    ): String {
        if (template.isEmpty()) return ""
        return template
            .replace("{nama}", nama)
            .replace("{barang}", barang)
            .replace("{tanggal}", tanggal)
            .replace("{total}", total)
            .replace("{dp}", dp)
            .replace("{sisa}", sisa)
            .replace("{invoice}", invoice)
            .replace("{toko}", toko)
            .replace("{catatan}", catatan.ifEmpty { "-" })
    }
}

/**
 * Teks Bawaan (Default Fallback) untuk Template Pesan WhatsApp & Nota
 */
object DefaultStoreConfig {
    const val NOTA_HEADER_DEFAULT = "" // Default header harus kosong
    const val NOTA_FOOTER_DEFAULT = "* Mohon tunjukkan nota ini saat pengembalian barang.\nTerima kasih telah menyewa alat di MyRent."

    val TEMPLATE_NOTA_DEFAULT = """
        *INVOICE TRANSAKSI SEWA - {toko}*
        ------------------------------------
        *No. Invoice* : {invoice}
        *Pelanggan*   : {nama}

        *RINCIAN SEWA*:
        • Barang      : {barang}
        • Periode     : {tanggal}
        • Total Biaya : {total}
        • Uang Muka   : {dp}
        • Sisa Bayar  : {sisa}
        • Jaminan/Note: {catatan}

        ------------------------------------
        *Catatan*: Mohon tunjukkan nota / pesan ini saat melakukan pengembalian barang.
        Terima kasih telah menyewa alat di MyRent!
    """.trimIndent()

    val TEMPLATE_H1_DEFAULT = "Halo {nama}, kami dari toko MyRent mengingatkan bahwa {barang} dijadwalkan dikembalikan besok, {tanggal}. Terima kasih."

    val TEMPLATE_H_DEFAULT = "Halo {nama}, hari ini jadwal pengembalian {barang}. Mohon dikembalikan hari ini ya. Terima kasih."
}
