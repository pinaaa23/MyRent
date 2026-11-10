package com.myrent.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myrent.app.R
import com.myrent.app.data.settings.LocalStoreProfileCache
import com.myrent.app.data.settings.StoreProfileDoc
import com.myrent.app.data.settings.StoreProfileRepository
import com.myrent.app.ui.beranda.FollowUpItem
import com.myrent.app.ui.beranda.HomeViewModel
import com.myrent.app.ui.components.AppBackground
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.GoldBorderColor
import com.myrent.app.ui.theme.OnAccentGold
import com.myrent.app.ui.theme.StatusGreen
import com.myrent.app.ui.theme.StatusRed
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Layar Utama Beranda (Dashboard Ringkasan & Daftar Perlu di-Follow Up WA Termasuk Keterlambatan)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit = {},
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val userDoc by homeViewModel.userDocState.collectAsState()
    val userNamaToko by homeViewModel.userNamaToko.collectAsState()

    val storeProfileRepo = remember { StoreProfileRepository() }
    val storeProfile by storeProfileRepo.getStoreProfileFlow().collectAsState(StoreProfileDoc())

    val displayNamaToko = storeProfile.nama.ifEmpty { userNamaToko }

    val sewaBerjalanCount by homeViewModel.sewaBerjalanCount.collectAsState()
    val jatuhTempoHariIniCount by homeViewModel.jatuhTempoHariIniCount.collectAsState()
    val omzetHariIni by homeViewModel.omzetHariIni.collectAsState()
    val followUpList by homeViewModel.followUpList.collectAsState()

    LaunchedEffect(userDoc, storeProfile, userNamaToko) {
        val nama = storeProfile.nama.ifEmpty { userNamaToko }
        val foto = storeProfile.fotoBase64.ifEmpty { userDoc.fotoProfilBase64 }
        if (nama.isNotEmpty()) {
            LocalStoreProfileCache.saveCache(context, nama, foto)
        }
    }

    AppBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (displayNamaToko.isNotEmpty()) "Selamat datang, $displayNamaToko!" else "Selamat datang di MyRent!",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(id = R.string.title_settings),
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Section 1: 3 Kartu Ringkasan (Dashboard)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kartu 1: Sewa Berjalan
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.summary_active_rentals),
                        value = "$sewaBerjalanCount",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        iconColor = AccentGold
                    )

                    // Kartu 2: Jatuh Tempo Hari Ini
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.summary_due_today),
                        value = "$jatuhTempoHariIniCount",
                        icon = Icons.Filled.Event,
                        iconColor = if (jatuhTempoHariIniCount > 0) StatusRed else TextSecondary
                    )

                    // Kartu 3: Omzet Hari Ini
                    SummaryCard(
                        modifier = Modifier.weight(1.2f),
                        title = stringResource(R.string.summary_revenue_today),
                        value = formatRupiah(omzetHariIni),
                        icon = Icons.Filled.MonetizationOn,
                        iconColor = StatusGreen
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Judul Daftar Perlu di-Follow Up
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.title_need_follow_up),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (followUpList.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentGold)
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${followUpList.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnAccentGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Section 3: List Follow-Up WA / Empty State
                if (followUpList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Aman",
                                tint = StatusGreen,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.empty_followup_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.empty_followup_desc),
                                fontSize = 14.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(followUpList, key = { "${it.rental.id}_${it.type}_${it.overdueDays}" }) { item ->
                            FollowUpCard(
                                item = item,
                                onKirimWaClick = {
                                    homeViewModel.kirimWaFollowUp(context, item)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Kartu Ringkasan Metric Dashboard Beranda (Sudut 20dp & Garis Tepi Emas 12% Alpha)
 */
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GoldBorderColor),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * Kartu Barang yang Perlu di-Follow Up WhatsApp (Sudut 20dp & Garis Tepi Emas 12% Alpha)
 */
@Composable
fun FollowUpCard(
    item: FollowUpItem,
    onKirimWaClick: () -> Unit
) {
    val rental = item.rental

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GoldBorderColor),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val tagBg = when (item.type) {
                        "TELAT" -> StatusRed
                        "Hari H" -> Color(0xFFD32F2F)
                        else -> AccentGold
                    }
                    val tagText = when (item.type) {
                        "TELAT" -> "Telat ${item.overdueDays} hari"
                        "Hari H" -> stringResource(R.string.tag_due_today)
                        else -> stringResource(R.string.tag_due_tomorrow)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tagBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tagText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.type == "TELAT" || item.type == "Hari H") Color.White else OnAccentGold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = rental.namaBarang,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${rental.namaPenyewa} (${rental.noWa})",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Kembali: ${formatDate(rental.tanggalKembali)}",
                    fontSize = 12.sp,
                    color = if (item.type == "TELAT") StatusRed else TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Tombol Kirim WA
            if (item.isFollowedUp) {
                OutlinedButton(
                    onClick = onKirimWaClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text(text = stringResource(R.string.status_followed_up), fontSize = 11.sp)
                }
            } else {
                Button(
                    onClick = onKirimWaClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim WA", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.btn_send_wa), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
