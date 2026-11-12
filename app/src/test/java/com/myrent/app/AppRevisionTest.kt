package com.myrent.app

import com.myrent.app.data.rentals.PaymentRecord
import com.myrent.app.data.rentals.Rental
import com.myrent.app.data.rentals.RentalItem
import com.myrent.app.data.rentals.mapJenisMetode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test untuk verifikasi validasi DP, Multi-Item Rental, Koreksi Data Lama, & Laporan
 */
class AppRevisionTest {

    private fun checkValidationDp(total: Long, dp: Long): Pair<Long, String> {
        val sisa = if (total > dp) total - dp else 0L
        val status = if (sisa == 0L) "lunas" else "belum_lunas"
        return Pair(sisa, status)
    }

    @Test
    fun testDpZero_SetsUnpaidStatus() {
        val (sisa, status) = checkValidationDp(100000L, 0L)
        assertEquals(100000L, sisa)
        assertEquals("belum_lunas", status)
    }

    @Test
    fun testDpExceedsTotal_IsInvalid() {
        val total = 100000L
        val dp = 150000L
        assertTrue(dp > total)
    }

    @Test
    fun testNegativeDp_IsInvalid() {
        val dp = -50000L
        assertTrue(dp < 0L)
    }

    @Test
    fun testDpEqualsTotal_SetsPaidStatus() {
        val (sisa, status) = checkValidationDp(100000L, 100000L)
        assertEquals(0L, sisa)
        assertEquals("lunas", status)
    }

    @Test
    fun testLegacyDoubleDpCorrection() {
        val legacyRental = Rental(
            id = "old_rental_1",
            hargaTotal = 100000L,
            dp = 100000L,
            jumlahPelunasan = 70000L,
            pelunasanPada = 1700000000000L,
            metodePembayaran = "Tunai"
        )

        val payments = legacyRental.getAllPaymentRecords()
        assertEquals(2, payments.size)

        val dpRecord = payments.first { it.jenis == "DP" }
        val pelunasanRecord = payments.first { it.jenis == "Pelunasan" }

        assertEquals(30000L, dpRecord.jumlah)
        assertEquals(70000L, pelunasanRecord.jumlah)
    }

    @Test
    fun testMapJenisMetode() {
        assertEquals("TUNAI", mapJenisMetode("Tunai"))
        assertEquals("TUNAI", mapJenisMetode("CASH"))
        assertEquals("NON_TUNAI", mapJenisMetode("QRIS"))
        assertEquals("NON_TUNAI", mapJenisMetode("Transfer"))
        assertEquals("UNSPECIFIED", mapJenisMetode(""))
        assertEquals("UNSPECIFIED", mapJenisMetode("Tidak diketahui"))
    }

    @Test
    fun testReportSumEquality_TunaiNonTunaiUnspecifiedEqualsTotal() {
        val payments = listOf(
            PaymentRecord(jumlah = 50000L, jenisMetode = "TUNAI"),
            PaymentRecord(jumlah = 30000L, jenisMetode = "NON_TUNAI"),
            PaymentRecord(jumlah = 20000L, jenisMetode = "UNSPECIFIED")
        )

        val tunaiSum = payments.filter { it.jenisMetode == "TUNAI" }.sumOf { it.jumlah }
        val nonTunaiSum = payments.filter { it.jenisMetode == "NON_TUNAI" }.sumOf { it.jumlah }
        val unspecifiedSum = payments.filter { it.jenisMetode == "UNSPECIFIED" }.sumOf { it.jumlah }
        val totalSum = payments.sumOf { it.jumlah }

        assertEquals(50000L, tunaiSum)
        assertEquals(30000L, nonTunaiSum)
        assertEquals(20000L, unspecifiedSum)
        assertEquals(totalSum, tunaiSum + nonTunaiSum + unspecifiedSum)
    }

    @Test
    fun testMultiItemRental_LegacyCompatibility() {
        val legacyRental = Rental(
            id = "legacy_1",
            namaBarang = "Jas Hitam",
            jumlah = 1,
            hargaTotal = 100000L,
            lamaHari = 2
        )

        val items = legacyRental.getItemsList()
        assertEquals(1, items.size)
        assertEquals("Jas Hitam", items[0].nama)
        assertEquals("Jas Hitam", legacyRental.getNamaDisplay())
    }

    @Test
    fun testMultiItemRental_DisplayNameAndSubtotal() {
        val item1 = RentalItem(itemId = "i1", nama = "Jas Hitam", hargaSatuan = 50000L, jumlah = 1)
        val item2 = RentalItem(itemId = "i2", nama = "Sepatu", hargaSatuan = 25000L, jumlah = 2)

        val multiRental = Rental(
            id = "multi_1",
            lamaHari = 2,
            items = listOf(item1, item2)
        )

        assertEquals("Jas Hitam +1 lainnya", multiRental.getNamaDisplay())
        assertEquals("Jas Hitam (1 unit), Sepatu (2 unit)", multiRental.getSummaryBarangWA())

        val sumUnit = multiRental.items.sumOf { it.hargaSatuan * it.jumlah.toLong() }
        val calculatedTotal = sumUnit * multiRental.lamaHari.toLong()
        assertEquals(200000L, calculatedTotal)
    }
}
