package com.myrent.app.ui.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.ui.screens.formatRupiah
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Komponen Grafik Batang Kustom menggunakan Canvas Compose (Tanpa Library Eksternal)
 */
@Composable
fun ReportBarChart(
    bars: List<BarChartItem>,
    selectedIndex: Int?,
    onBarSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) return

    val maxValue = bars.maxOfOrNull { it.value } ?: 1L
    val effectiveMax = if (maxValue <= 0L) 1L else maxValue

    val selectedBar = if (selectedIndex != null && selectedIndex in bars.indices) {
        bars[selectedIndex]
    } else null

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Tooltip Keterangan Batang Terpilih
            if (selectedBar != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedBar.fullLabel,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = formatRupiah(selectedBar.value),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(28.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Batang Grafik
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .pointerInput(bars) {
                            detectTapGestures { offset ->
                                val barWidth = size.width / bars.size
                                val tappedIndex = (offset.x / barWidth).toInt().coerceIn(0, bars.lastIndex)
                                onBarSelected(tappedIndex)
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barCount = bars.size
                    val totalSpacing = canvasWidth * 0.25f
                    val barWidth = (canvasWidth - totalSpacing) / barCount
                    val spacing = totalSpacing / (barCount + 1)

                    bars.forEachIndexed { index, bar ->
                        val left = spacing + index * (barWidth + spacing)

                        // Hitung tinggi proporsional (minimal 8px jika 0 agar tetap terlihat)
                        val minBarHeight = 8f
                        val heightFraction = if (effectiveMax > 0) bar.value.toFloat() / effectiveMax.toFloat() else 0f
                        val barHeight = (canvasHeight * heightFraction * 0.85f).coerceAtLeast(minBarHeight)
                        val top = canvasHeight - barHeight

                        val isSelected = index == selectedIndex
                        val color = if (isSelected) AccentGold else Color(0xFF3A3A3C)

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Label Sumbu X di Bawah Grafik
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                bars.forEachIndexed { index, bar ->
                    val isSelected = index == selectedIndex
                    Text(
                        text = bar.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) AccentGold else TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onBarSelected(index) }
                    )
                }
            }
        }
    }
}
