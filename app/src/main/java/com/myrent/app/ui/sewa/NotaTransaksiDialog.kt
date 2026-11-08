package com.myrent.app.ui.sewa

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.R
import com.myrent.app.data.rentals.Rental
import com.myrent.app.ui.screens.formatDate
import com.myrent.app.ui.screens.formatRupiah
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog Layar Nota Transaksi Profesional (Struk Termal Kasir LENGKAP)
 */
@Composable
fun NotaTransaksiDialog(
    rental: Rental,
    kasirNama: String,
    cabangNama: String,
    cabangAlamat: String,
    cabangNoWa: String,
    viewModel: RentalViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val tglSewaStr = remember(rental.dibuatPada) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        sdf.format(Date(rental.dibuatPada))
    }

    val invoiceId = remember(rental.dibuatPada, rental.id) {
        if (rental.invoiceId.isNotEmpty()) rental.invoiceId
        else {
            val sdfId = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            "INV-${sdfId.format(Date(rental.dibuatPada))}"
        }
    }

    val tglMulaiStr = remember(rental.tanggalMulai) { formatDate(rental.tanggalMulai) }
    val tglKembaliStr = remember(rental.tanggalKembali) { formatDate(rental.tanggalKembali) }

    val statusBayarStr = if (rental.sisaBayar == 0L) "LUNAS" else "BELUM LUNAS"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Top Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "Home",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.title_receipt),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 40.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Kartu Struk Termal Putih Lengkap
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Kop Toko
                    Text(
                        text = "MYRENT STORE - ${cabangNama.ifEmpty { "PUSAT" }}".uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Telp/WA: ${cabangNoWa.ifEmpty { "08123456789" }}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )
                    Text(
                        text = cabangAlamat.ifEmpty { "Jl. Janti-Yogyakarta" },
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    ReceiptDivider()

                    // Data Pelanggan & Transaksi
                    ReceiptInfoLine("No Invoice", ": $invoiceId")
                    ReceiptInfoLine("Waktu", ": $tglSewaStr")
                    ReceiptInfoLine("Kasir", ": ${kasirNama.ifEmpty { "Admin MyRent" }}")
                    ReceiptInfoLine("Pelanggan", ": ${rental.namaPenyewa.uppercase()}")
                    ReceiptInfoLine("No. WA", ": ${rental.noWa}")

                    ReceiptDivider()

                    // Periode Peminjaman & Pengembalian
                    ReceiptInfoLine("Tgl Pinjam", ": $tglMulaiStr")
                    ReceiptInfoLine("Tgl Kembali", ": $tglKembaliStr")
                    ReceiptInfoLine("Lama Sewa", ": ${rental.lamaHari} Hari")

                    ReceiptDivider()

                    // Rincian Layanan / Barang
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "LAYANAN SEWA:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "1. ${rental.namaBarang}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }
                    val unitHarga = if (rental.jumlah > 0 && rental.lamaHari > 0) rental.hargaTotal / (rental.jumlah * rental.lamaHari) else rental.hargaTotal
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "   ${rental.jumlah} unit x ${formatRupiah(unitHarga)} x ${rental.lamaHari} hr",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                        Text(
                            text = formatRupiah(rental.hargaTotal),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }

                    ReceiptDivider()

                    // Biaya, DP, Sisa, Metode, & Status Lunas
                    ReceiptInfoLine("Total Biaya", ": ${formatRupiah(rental.hargaTotal)}")
                    ReceiptInfoLine("Uang Muka", ": ${formatRupiah(rental.dp)}")
                    ReceiptInfoLine("Sisa Bayar", ": ${formatRupiah(rental.sisaBayar)}")
                    ReceiptInfoLine("Metode", ": ${rental.metodePembayaran}")
                    ReceiptInfoLine("Status", ": $statusBayarStr")

                    ReceiptDivider()

                    // Catatan / Jaminan
                    if (rental.catatan.isNotEmpty()) {
                        ReceiptInfoLine("Jaminan/Note", ": ${rental.catatan}")
                        ReceiptDivider()
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Catatan Pengembalian
                    Text(
                        text = "* Mohon tunjukkan nota ini saat pengembalian barang.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Terima kasih telah menyewa alat di MyRent.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Baris Tombol Aksi (Kirim WA & Cetak Nota)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tombol Kirim WA Nota
                Button(
                    onClick = { viewModel.kirimWhatsAppNota(context, rental, cabangNama) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim WA", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.btn_send_wa), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Tombol Cetak Nota
                Button(
                    onClick = { cetakStrukProfesional(context, rental, invoiceId, tglSewaStr, tglMulaiStr, tglKembaliStr, cabangNama, cabangAlamat, cabangNoWa, kasirNama) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Print, contentDescription = "Cetak Nota", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.btn_print_receipt), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tombol Kembali ke Dashboard
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.btn_back_to_dashboard),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ReceiptDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - -",
        color = Color.Gray,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun ReceiptInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.Black,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Fungsi Cetak Struk Profesional via Android PrintManager
 */
fun cetakStrukProfesional(
    context: Context,
    rental: Rental,
    invoiceId: String,
    tglSewaStr: String,
    tglMulaiStr: String,
    tglKembaliStr: String,
    cabangNama: String,
    cabangAlamat: String,
    cabangNoWa: String,
    kasirNama: String
) {
    try {
        val unitHarga = if (rental.jumlah > 0 && rental.lamaHari > 0) rental.hargaTotal / (rental.jumlah * rental.lamaHari) else rental.hargaTotal
        val statusStr = if (rental.sisaBayar == 0L) "LUNAS" else "BELUM LUNAS"

        val htmlContent = """
            <html>
            <head>
                <style>
                    body { font-family: monospace; font-size: 13px; padding: 15px; }
                    .center { text-align: center; }
                    .divider { border-bottom: 1px dashed #000; margin: 8px 0; }
                    .bold { font-weight: bold; }
                    .flex { display: flex; justify-content: space-between; }
                </style>
            </head>
            <body>
                <div class="center bold" style="font-size: 16px;">MYRENT STORE - ${cabangNama.ifEmpty { "PUSAT" }}</div>
                <div class="center">Telp/WA: ${cabangNoWa.ifEmpty { "08123456789" }}</div>
                <div class="center">${cabangAlamat.ifEmpty { "Jl. Janti-Yogyakarta" }}</div>
                <div class="divider"></div>
                <div>No Invoice : <span class="bold">$invoiceId</span></div>
                <div>Waktu &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;: $tglSewaStr</div>
                <div>Kasir &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;: ${kasirNama.ifEmpty { "Admin MyRent" }}</div>
                <div>Pelanggan &nbsp;: <span class="bold">${rental.namaPenyewa.uppercase()}</span></div>
                <div>No. WA &nbsp;&nbsp;&nbsp;&nbsp;: ${rental.noWa}</div>
                <div class="divider"></div>
                <div>Tgl Pinjam &nbsp;: $tglMulaiStr</div>
                <div>Tgl Kembali: $tglKembaliStr</div>
                <div>Lama Sewa &nbsp;: ${rental.lamaHari} Hari</div>
                <div class="divider"></div>
                <div class="bold">LAYANAN SEWA:</div>
                <div class="bold">1. ${rental.namaBarang}</div>
                <div class="flex">
                    <span>&nbsp;&nbsp;&nbsp;${rental.jumlah} unit x ${formatRupiah(unitHarga)} x ${rental.lamaHari} hr</span>
                    <span class="bold">${formatRupiah(rental.hargaTotal)}</span>
                </div>
                <div class="divider"></div>
                <div class="flex">
                    <span>Total Biaya</span>
                    <span class="bold">${formatRupiah(rental.hargaTotal)}</span>
                </div>
                <div class="flex">
                    <span>Uang Muka/DP</span>
                    <span>${formatRupiah(rental.dp)}</span>
                </div>
                <div class="flex">
                    <span>Sisa Bayar</span>
                    <span class="bold">${formatRupiah(rental.sisaBayar)}</span>
                </div>
                <div class="flex">
                    <span>Metode</span>
                    <span>${rental.metodePembayaran}</span>
                </div>
                <div class="flex">
                    <span>Status</span>
                    <span class="bold">$statusStr</span>
                </div>
                <div class="divider"></div>
                ${if (rental.catatan.isNotEmpty()) "<div>Jaminan/Note: ${rental.catatan}</div><div class='divider'></div>" else ""}
                <br/>
                <div class="center bold">* Mohon tunjukkan nota ini saat pengembalian barang.</div>
                <div class="center">Terima kasih telah menyewa alat di MyRent.</div>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(invoiceId)
                printManager.print(invoiceId, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
