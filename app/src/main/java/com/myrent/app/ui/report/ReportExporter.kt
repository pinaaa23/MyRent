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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Helper untuk mengekspor Laporan Omzet & Transaksi ke file Murni Excel (.xlsx) OpenXML
 * dengan 2 Sheet ("Ringkasan" & "Transaksi") yang dapat langsung dibuka berkolom rapi di Microsoft Excel, Google Sheets, & WPS Office.
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
    ) {
        try {
            val sdfDate = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val nowStr = sdfDate.format(Date())
            val periodName = periodType.name.lowercase()

            val fileName = "MyRent-Laporan-$periodName-$nowStr.xlsx"
            val exportDateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date())

            // Buat file .xlsx murni (OpenXML ZIP) dengan Sheet1 (Ringkasan) & Sheet2 (Transaksi)
            val xlsxBytes = buildXlsxBytes(
                namaToko = namaToko.ifEmpty { "MYRENT STORE" },
                periodRangeText = periodRangeText,
                exportDateStr = exportDateStr,
                omzetKotor = omzetKotor,
                totalTransaksi = totalTransaksi,
                sudahDiterima = sudahDiterima,
                sudahDiterimaTunai = sudahDiterimaTunai,
                sudahDiterimaNonTunai = sudahDiterimaNonTunai,
                belumDibayar = belumDibayar,
                rentals = rentals
            )

            // Simpan otomatis ke folder Download publik HP
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Gagal membuat file di folder Downloads.")
                resolver.openOutputStream(uri)?.use { os ->
                    os.write(xlsxBytes)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                val fos = FileOutputStream(file)
                fos.write(xlsxBytes)
                fos.flush()
                fos.close()
            }

            Toast.makeText(context, "File laporan $fileName berhasil diunduh ke folder Download", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal mengunduh file laporan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildXlsxBytes(
        namaToko: String,
        periodRangeText: String,
        exportDateStr: String,
        omzetKotor: Long,
        totalTransaksi: Int,
        sudahDiterima: Long,
        sudahDiterimaTunai: Long,
        sudahDiterimaNonTunai: Long,
        belumDibayar: Long,
        rentals: List<Rental>
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)

        fun addZipFile(path: String, content: String) {
            val entry = ZipEntry(path)
            zos.putNextEntry(entry)
            zos.write(content.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        // 1. [Content_Types].xml
        addZipFile(
            "[Content_Types].xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
            </Types>""".trimIndent()
        )

        // 2. _rels/.rels
        addZipFile(
            "_rels/.rels",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>""".trimIndent()
        )

        // 3. xl/_rels/workbook.xml.rels
        addZipFile(
            "xl/_rels/workbook.xml.rels",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
            </Relationships>""".trimIndent()
        )

        // 4. xl/workbook.xml
        addZipFile(
            "xl/workbook.xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <sheets>
                    <sheet name="Ringkasan" sheetId="1" r:id="rId1"/>
                    <sheet name="Transaksi" sheetId="2" r:id="rId2"/>
                </sheets>
            </workbook>""".trimIndent()
        )

        // 5. xl/styles.xml
        addZipFile(
            "xl/styles.xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <fonts count="2">
                    <font><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><sz val="11"/><name val="Calibri"/></font>
                </fonts>
                <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
                <borders count="1"><border/></borders>
                <cellStyleXfs count="1"><xf borderId="0" fillId="0" fontId="0"/></cellStyleXfs>
                <cellXfs count="2">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"/>
                </cellXfs>
            </styleSheet>""".trimIndent()
        )

        fun escapeXml(str: String): String {
            return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
        }

        fun makeCell(colStr: String, rowIdx: Int, value: String, isBold: Boolean = false): String {
            val sAttr = if (isBold) " s=\"1\"" else ""
            val escaped = escapeXml(value)
            return """<c r="$colStr$rowIdx" t="inlineStr"$sAttr><is><t>$escaped</t></is></c>"""
        }

        // 6. xl/worksheets/sheet1.xml (Ringkasan)
        val s1Rows = StringBuilder()
        s1Rows.append("""<row r="1">${makeCell("A", 1, "RINGKASAN LAPORAN MYRENT", true)}</row>""")
        s1Rows.append("""<row r="2">${makeCell("A", 2, "Nama Toko", true)}${makeCell("B", 2, namaToko)}</row>""")
        s1Rows.append("""<row r="3">${makeCell("A", 3, "Periode Laporan", true)}${makeCell("B", 3, periodRangeText)}</row>""")
        s1Rows.append("""<row r="4">${makeCell("A", 4, "Tanggal Export", true)}${makeCell("B", 4, exportDateStr)}</row>""")
        s1Rows.append("""<row r="5"></row>""")
        s1Rows.append("""<row r="6">${makeCell("A", 6, "METRIK LAPORAN", true)}${makeCell("B", 6, "NILAI", true)}</row>""")
        s1Rows.append("""<row r="7">${makeCell("A", 7, "Omzet Kotor")}${makeCell("B", 7, formatRupiah(omzetKotor))}</row>""")
        s1Rows.append("""<row r="8">${makeCell("A", 8, "Jumlah Transaksi")}${makeCell("B", 8, totalTransaksi.toString())}</row>""")
        s1Rows.append("""<row r="9">${makeCell("A", 9, "Sudah Diterima (Total)")}${makeCell("B", 9, formatRupiah(sudahDiterima))}</row>""")
        s1Rows.append("""<row r="10">${makeCell("A", 10, "  - Diterima Tunai")}${makeCell("B", 10, formatRupiah(sudahDiterimaTunai))}</row>""")
        s1Rows.append("""<row r="11">${makeCell("A", 11, "  - Diterima Non-Tunai")}${makeCell("B", 11, formatRupiah(sudahDiterimaNonTunai))}</row>""")
        s1Rows.append("""<row r="12">${makeCell("A", 12, "Belum Dibayar")}${makeCell("B", 12, formatRupiah(belumDibayar))}</row>""")

        addZipFile(
            "xl/worksheets/sheet1.xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData>
                    $s1Rows
                </sheetData>
            </worksheet>""".trimIndent()
        )

        // 7. xl/worksheets/sheet2.xml (Transaksi)
        val s2Rows = StringBuilder()
        s2Rows.append(
            """<row r="1">
                ${makeCell("A", 1, "No", true)}
                ${makeCell("B", 1, "No. Nota", true)}
                ${makeCell("C", 1, "Tanggal Pinjam", true)}
                ${makeCell("D", 1, "Tanggal Kembali", true)}
                ${makeCell("E", 1, "Nama Penyewa", true)}
                ${makeCell("F", 1, "Nomor WA", true)}
                ${makeCell("G", 1, "Barang Disewa", true)}
                ${makeCell("H", 1, "Total Biaya", true)}
                ${makeCell("I", 1, "Uang Muka (DP)", true)}
                ${makeCell("J", 1, "Sisa Bayar", true)}
                ${makeCell("K", 1, "Metode Pembayaran", true)}
                ${makeCell("L", 1, "Status", true)}
            </row>""".trimIndent()
        )

        rentals.forEachIndexed { index, rental ->
            val rowIdx = index + 2
            val no = index + 1
            val invoice = rental.invoiceId.ifEmpty { "INV-${rental.dibuatPada}" }
            val tglMulai = formatDate(rental.tanggalMulai)
            val tglKembali = formatDate(rental.tanggalKembali)
            val penyewa = rental.namaPenyewa
            val noWa = rental.noWa
            val barang = rental.getSummaryBarangWA()
            val total = formatRupiah(rental.hargaTotal)
            val dp = formatRupiah(rental.dp)
            val sisa = formatRupiah(rental.sisaBayar)
            val metode = if (rental.dp == 0L && rental.jumlahPelunasan == 0L) "-" else rental.metodePembayaran.ifEmpty { "-" }
            val status = if (rental.sisaBayar == 0L) "LUNAS" else "BELUM LUNAS"

            s2Rows.append(
                """<row r="$rowIdx">
                    ${makeCell("A", rowIdx, no.toString())}
                    ${makeCell("B", rowIdx, invoice)}
                    ${makeCell("C", rowIdx, tglMulai)}
                    ${makeCell("D", rowIdx, tglKembali)}
                    ${makeCell("E", rowIdx, penyewa)}
                    ${makeCell("F", rowIdx, noWa)}
                    ${makeCell("G", rowIdx, barang)}
                    ${makeCell("H", rowIdx, total)}
                    ${makeCell("I", rowIdx, dp)}
                    ${makeCell("J", rowIdx, sisa)}
                    ${makeCell("K", rowIdx, metode)}
                    ${makeCell("L", rowIdx, status)}
                </row>""".trimIndent()
            )
        }

        addZipFile(
            "xl/worksheets/sheet2.xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData>
                    $s2Rows
                </sheetData>
            </worksheet>""".trimIndent()
        )

        zos.flush()
        zos.close()

        return baos.toByteArray()
    }
}
