package com.myrent.app.ui.report

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.myrent.app.data.rentals.Rental
import com.myrent.app.ui.screens.formatDate
import com.myrent.app.ui.screens.formatRupiah
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper untuk mengunduh Laporan Omzet & Transaksi ke folder Download perangkat (.csv UTF-8 BOM)
 */
object ReportExporter {

    fun generateAndSaveToDownloads(
        context: Context,
        namaToko: String,
        periodType: PeriodType,
        periodRangeText: String,
        omzetKotor: Long,
        totalTransaksi: Int,
        sudahDiterima: Long,
        sudahDiterimaTunai: Long,
        sudahDiterimaNonTunai: Long,
        belumDibayar: Long,
        rentals: List<Rental>
    ): Result<Unit> = runCatching {
        val sdfDate = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val nowStr = sdfDate.format(Date())
        val periodName = periodType.name.lowercase()

        val fileName = "MyRent-Laporan-$periodName-$nowStr.csv"
        val exportDateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date())

        val sb = StringBuilder()
        // Tulis Byte Order Mark (BOM) UTF-8 agar Excel mengenali encoding secara otomatis
        sb.append("\uFEFF")

        // SECTION 1: SHEET RINGKASAN LAPORAN
        sb.append("========================================\n")
        sb.append("RINGKASAN LAPORAN MYRENT\n")
        sb.append("========================================\n")
        sb.append("Nama Toko;${namaToko.ifEmpty { "MYRENT STORE" }}\n")
        sb.append("Periode Laporan;${periodRangeText}\n")
        sb.append("Tanggal Export;${exportDateStr}\n")
        sb.append("\n")
        sb.append("METRIK LAPORAN;NILAI\n")
        sb.append("Omzet Kotor;${formatRupiah(omzetKotor)}\n")
        sb.append("Jumlah Transaksi;${totalTransaksi}\n")
        sb.append("Sudah Diterima (Total);${formatRupiah(sudahDiterima)}\n")
        sb.append("  - Diterima Tunai;${formatRupiah(sudahDiterimaTunai)}\n")
        sb.append("  - Diterima Non-Tunai;${formatRupiah(sudahDiterimaNonTunai)}\n")
        sb.append("Belum Dibayar;${formatRupiah(belumDibayar)}\n")
        sb.append("\n\n")

        // SECTION 2: SHEET DAFTAR TRANSAKSI SEWA
        sb.append("========================================\n")
        sb.append("DAFTAR TRANSAKSI SEWA ($totalTransaksi)\n")
        sb.append("========================================\n")
        sb.append("No;No. Nota;Tanggal Pinjam;Tanggal Kembali;Nama Penyewa;Nomor WA;Barang Disewa;Total Biaya;Uang Muka (DP);Sisa Bayar;Metode Pembayaran;Status\n")

        rentals.forEachIndexed { index, rental ->
            val no = index + 1
            val invoice = rental.invoiceId.ifEmpty { "INV-${rental.dibuatPada}" }
            val tglMulai = formatDate(rental.tanggalMulai)
            val tglKembali = formatDate(rental.tanggalKembali)
            val penyewa = rental.namaPenyewa.replace(";", ",")
            val noWa = rental.noWa.replace(";", ",")
            val barang = rental.getSummaryBarangWA().replace(";", ",")
            val total = formatRupiah(rental.hargaTotal)
            val dp = formatRupiah(rental.dp)
            val sisa = formatRupiah(rental.sisaBayar)
            val metode = if (rental.dp == 0L && rental.jumlahPelunasan == 0L) "-" else rental.metodePembayaran.ifEmpty { "-" }
            val status = if (rental.sisaBayar == 0L) "LUNAS" else "BELUM LUNAS"

            sb.append("$no;$invoice;$tglMulai;$tglKembali;$penyewa;$noWa;$barang;$total;$dp;$sisa;$metode;$status\n")
        }

        val fileBytes = sb.toString().toByteArray(Charsets.UTF_8)

        // Simpan otomatis ke folder Download publik HP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Gagal membuat file di folder Downloads.")
            resolver.openOutputStream(uri)?.use { os ->
                os.write(fileBytes)
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            val fos = FileOutputStream(file)
            val writer = OutputStreamWriter(fos, Charsets.UTF_8)
            writer.write(sb.toString())
            writer.flush()
            writer.close()
            fos.close()
        }

        Toast.makeText(context, "File laporan $fileName berhasil diunduh ke folder Download", Toast.LENGTH_LONG).show()
    }
}
