package com.myrent.app.ui.sewa

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.R
import com.myrent.app.data.foto.FotoRepository
import com.myrent.app.data.rentals.Rental
import com.myrent.app.data.settings.DefaultStoreConfig
import com.myrent.app.data.settings.StoreProfileDoc
import com.myrent.app.data.settings.StoreProfileRepository
import com.myrent.app.ui.screens.formatDate
import com.myrent.app.ui.screens.formatRupiah
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog Layar Nota Transaksi Profesional (Struk Termal Kasir LENGKAP dengan Logo, Alamat, No WA, & Kustom Header/Footer)
 */
@Composable
fun NotaTransaksiDialog(
    rental: Rental,
    kasirNama: String,
    cabangNama: String = "",
    cabangAlamat: String = "",
    cabangNoWa: String = "",
    viewModel: RentalViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val userDoc by viewModel.userDocState.collectAsState()
    val storeConfigDoc by viewModel.storeConfig.collectAsState()

    val storeProfileRepo = remember { StoreProfileRepository() }
    val storeProfile by storeProfileRepo.getStoreProfileFlow().collectAsState(StoreProfileDoc())

    val fotoRepo = remember { FotoRepository() }

    val namaToko = storeProfile.nama.ifEmpty { userDoc.namaToko.ifEmpty { cabangNama.ifEmpty { "MYRENT STORE" } } }
    val alamatToko = storeProfile.alamat.ifEmpty { userDoc.alamatToko.ifEmpty { cabangAlamat } }
    val noWaToko = storeProfile.noTelp.ifEmpty { userDoc.noWaToko.ifEmpty { cabangNoWa } }
    val headerText = storeConfigDoc.notaHeader.ifEmpty { DefaultStoreConfig.NOTA_HEADER_DEFAULT }
    val footerText = storeConfigDoc.notaFooter.ifEmpty { DefaultStoreConfig.NOTA_FOOTER_DEFAULT }
    val fotoBase64 = storeProfile.fotoBase64.ifEmpty { userDoc.fotoProfilBase64 }

    val tglSewaStr = remember(rental.dibuatPada) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        sdf.format(Date(rental.dibuatPada))
    }

    val invoiceId = remember(rental.dibuatPada, rental.id) {
        if (rental.invoiceId.isNotEmpty()) rental.invoiceId
        else {
            val sdfId = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
            "NOTA-${sdfId.format(Date(rental.dibuatPada))}"
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
                    // Logo Toko (jika ada)
                    val bitmap = remember(fotoBase64) {
                        if (fotoBase64.isNotEmpty()) fotoRepo.base64ToBitmap(fotoBase64) else null
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Logo Struk",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Kop Nama Toko
                    Text(
                        text = namaToko.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    // Header Teks Kop
                    if (headerText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = headerText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                    }

                    // No WA / Telepon Toko
                    if (noWaToko.isNotEmpty()) {
                        Text(
                            text = "Telp/WA: $noWaToko",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }

                    // Alamat Toko
                    if (alamatToko.isNotEmpty()) {
                        Text(
                            text = alamatToko,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }

                    ReceiptDivider()

                    // Data Pelanggan & Transaksi
                    ReceiptInfoLine("No. Nota", ": $invoiceId")
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

                    // Rincian Layanan / Barang (Multi-Item)
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

                    rental.getItemsList().forEachIndexed { idx, rItem ->
                        val subtotalItem = rItem.hargaSatuan * rItem.jumlah.toLong() * rental.lamaHari.toLong()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "${idx + 1}. ${rItem.nama}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "   ${rItem.jumlah} unit x ${formatRupiah(rItem.hargaSatuan)} x ${rental.lamaHari} hr",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                            Text(
                                text = formatRupiah(subtotalItem),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    ReceiptDivider()

                    val displayMetode = if (rental.dp == 0L && rental.jumlahPelunasan == 0L) "-" else rental.metodePembayaran.ifEmpty { "-" }

                    // Biaya, DP, Sisa, Metode, & Status Lunas
                    ReceiptInfoLine("Total Biaya", ": ${formatRupiah(rental.hargaTotal)}")
                    ReceiptInfoLine("Uang Muka", ": ${formatRupiah(rental.dp)}")
                    ReceiptInfoLine("Sisa Bayar", ": ${formatRupiah(rental.sisaBayar)}")
                    ReceiptInfoLine("Metode", ": $displayMetode")
                    ReceiptInfoLine("Status", ": $statusBayarStr")

                    ReceiptDivider()

                    // Jaminan
                    if (rental.catatan.isNotEmpty()) {
                        ReceiptInfoLine("Jaminan", ": ${rental.catatan}")
                        ReceiptDivider()
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Footer Struk Kustom
                    Text(
                        text = footerText,
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
                    onClick = { viewModel.kirimWhatsAppNota(context, rental) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim WA", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.btn_send_wa), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Tombol Cetak Nota (Warna Teks Hitam Jelas di Atas Latar Putih)
                Button(
                    onClick = { cetakStrukProfesional(context, rental, invoiceId, tglSewaStr, tglMulaiStr, tglKembaliStr, namaToko, alamatToko, noWaToko, kasirNama, headerText, footerText, fotoBase64) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Print, contentDescription = "Cetak Nota", tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.btn_print_receipt), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
 * Fungsi Cetak Struk Profesional via Android PrintManager dengan Logo, Alamat, WA, & Kustom Header/Footer
 */
fun cetakStrukProfesional(
    context: Context,
    rental: Rental,
    invoiceId: String,
    tglSewaStr: String,
    tglMulaiStr: String,
    tglKembaliStr: String,
    namaToko: String,
    alamatToko: String,
    noWaToko: String,
    kasirNama: String,
    headerText: String,
    footerText: String,
    fotoBase64: String
) {
    try {
        val statusStr = if (rental.sisaBayar == 0L) "LUNAS" else "BELUM LUNAS"
        val displayMetode = if (rental.dp == 0L && rental.jumlahPelunasan == 0L) "-" else rental.metodePembayaran.ifEmpty { "-" }

        val logoHtml = if (fotoBase64.isNotEmpty()) {
            "<div class='center'><img src='data:image/jpeg;base64,$fotoBase64' style='max-width: 80px; max-height: 80px; filter: grayscale(100%) contrast(200%); margin-bottom: 5px;' /></div>"
        } else ""

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
                $logoHtml
                <div class="center bold" style="font-size: 16px;">${namaToko.ifEmpty { "MYRENT STORE" }.uppercase()}</div>
                ${if (headerText.isNotEmpty()) "<div class='center' style='font-size: 11px;'>$headerText</div>" else ""}
                ${if (noWaToko.isNotEmpty()) "<div class='center'>Telp/WA: $noWaToko</div>" else ""}
                ${if (alamatToko.isNotEmpty()) "<div class='center'>$alamatToko</div>" else ""}
                <div class="divider"></div>
                <div>No. Nota &nbsp;&nbsp;: <span class="bold">$invoiceId</span></div>
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
                ${
                    rental.getItemsList().mapIndexed { idx, rItem ->
                        val subtotalItem = rItem.hargaSatuan * rItem.jumlah.toLong() * rental.lamaHari.toLong()
                        """
                        <div class="bold">${idx + 1}. ${rItem.nama}</div>
                        <div class="flex">
                            <span>&nbsp;&nbsp;&nbsp;${rItem.jumlah} unit x ${formatRupiah(rItem.hargaSatuan)} x ${rental.lamaHari} hr</span>
                            <span class="bold">${formatRupiah(subtotalItem)}</span>
                        </div>
                        """.trimIndent()
                    }.joinToString("<div style='height: 4px;'></div>")
                }
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
                    <span>$displayMetode</span>
                </div>
                <div class="flex">
                    <span>Status</span>
                    <span class="bold">$statusStr</span>
                </div>
                <div class="divider"></div>
                ${if (rental.catatan.isNotEmpty()) "<div>Jaminan: ${rental.catatan}</div><div class='divider'></div>" else ""}
                <br/>
                <div class="center" style="white-space: pre-line;">${footerText.ifEmpty { DefaultStoreConfig.NOTA_FOOTER_DEFAULT }}</div>
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
