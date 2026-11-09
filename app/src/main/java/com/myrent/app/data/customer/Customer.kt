package com.myrent.app.data.customer

/**
 * Model data Pelanggan / Penyewa ("customers")
 */
data class Customer(
    val id: String = "",          // Document ID: "{cabangId}_{noWaNorm}"
    val cabangId: String = "",
    val nama: String = "",
    val noWa: String = "",        // Nomor WA dinormalkan (misal: "08123456789")
    val dibuatPada: Long = System.currentTimeMillis(),
    val diubahPada: Long = System.currentTimeMillis()
)

/**
 * Fungsi penormalan nomor WhatsApp:
 * - Menghapus semua karakter non-angka
 * - Mengubah awalan "62" atau "+62" menjadi "0"
 */
fun normalizeNoWa(phone: String): String {
    var digits = phone.replace("[^0-9]".toRegex(), "")
    if (digits.startsWith("62")) {
        digits = "0" + digits.substring(2)
    }
    return digits
}

/**
 * Validasi nomor WA: Harus 10 sampai 14 digit angka
 */
fun isValidNoWa(phone: String): Boolean {
    val norm = normalizeNoWa(phone)
    return norm.length in 10..14
}
