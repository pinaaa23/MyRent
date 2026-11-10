package com.myrent.app.data.settings

/**
 * Model data konfigurasi kustom nota & template pesan follow-up WA toko
 * Disimpan di Firestore "toko/{uid}/pengaturan/nota"
 */
data class NotaConfigDoc(
    val judulNota: String = "NOTA",
    val kalimatPembuka: String = "",
    val catatanBawah: String = "",
    val flagTampilAlamat: Boolean = true,
    val flagTampilTelp: Boolean = true,
    val templateFollowUpH1: String = "",
    val templateFollowUpHariH: String = "",
    val templateFollowUpTelat: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
