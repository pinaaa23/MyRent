package com.myrent.app

import com.myrent.app.data.customer.isValidNoWa
import com.myrent.app.data.customer.normalizeNoWa
import com.myrent.app.data.settings.DefaultStoreConfig
import com.myrent.app.data.settings.TemplateReplacer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk pengujian penggantian variabel template pesan WA & normalisasi nomor WA
 */
class TemplateReplacerTest {

    @Test
    fun testTemplateVariableReplacement() {
        val template = DefaultStoreConfig.TEMPLATE_H1_DEFAULT
        val result = TemplateReplacer.replaceVariables(
            template = template,
            nama = "Fauzan",
            barang = "Kamera Canon EOS 80D (1 unit)",
            tanggal = "10 Nov 2026"
        )

        assertEquals("Halo Fauzan, kami dari toko MyRent mengingatkan bahwa Kamera Canon EOS 80D (1 unit) dijadwalkan dikembalikan besok, 10 Nov 2026. Terima kasih.", result)
    }

    @Test
    fun testUnknownVariablesKeptAsIs() {
        val customTemplate = "Halo {nama}, diskon voucher kamu adalah {kode_voucher_diskon} untuk sewa {barang}."
        val result = TemplateReplacer.replaceVariables(
            template = customTemplate,
            nama = "Fauzan",
            barang = "Kamera Canon"
        )

        // Variabel {kode_voucher_diskon} yang tidak dikenal tetap utuh tanpa crash
        assertEquals("Halo Fauzan, diskon voucher kamu adalah {kode_voucher_diskon} untuk sewa Kamera Canon.", result)
    }

    @Test
    fun testEmptyTemplateHandledGracefully() {
        val result = TemplateReplacer.replaceVariables(
            template = "",
            nama = "Fauzan"
        )

        assertEquals("", result)
    }

    @Test
    fun testNormalizeNoWa() {
        assertEquals("08123456789", normalizeNoWa("+628123456789"))
        assertEquals("08123456789", normalizeNoWa("628123456789"))
        assertEquals("08123456789", normalizeNoWa("08123456789"))
        assertEquals("08123456789", normalizeNoWa("0812-3456-789"))
    }

    @Test
    fun testIsValidNoWa() {
        assertTrue(isValidNoWa("08123456789"))     // 11 digit
        assertTrue(isValidNoWa("6281234567890"))   // 13 digit
        assertFalse(isValidNoWa("08123"))           // 5 digit (terlalu pendek)
        assertFalse(isValidNoWa("08123456789012345")) // 17 digit (terlalu panjang)
    }
}
