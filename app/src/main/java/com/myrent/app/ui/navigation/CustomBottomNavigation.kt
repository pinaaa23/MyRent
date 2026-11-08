package com.myrent.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.NavActiveCircle
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Navigation Bar Custom (bukan NavigationBar bawaan)
 * Sesuai referensi nav-bar.jpg versi sederhana:
 * - Bar rata dengan sudut atas membulat (tanpa notch)
 * - 4 menu berurutan: Beranda, Sewa, Katalog, Laporan
 * - Menu aktif: lingkaran abu (#2F2F2F) di belakang ikon, ikon & label putih gading (#F2EFE8), label tebal (Bold)
 * - Menu tidak aktif: ikon & label abu (#A8A8A8)
 * - Animasi perpindahan halus dan singkat (maksimal 200 ms)
 * - Area sentuh minimal 48dp, ukuran label minimal 12sp
 */
@Composable
fun CustomBottomNavigation(
    currentRoute: String?,
    onItemSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Screen.Home,
        Screen.Rental,
        Screen.Catalog,
        Screen.Report
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(DarkSurface)
            .height(72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { screen ->
                val isSelected = currentRoute == screen.route

                // Animasi transisi warna lingkaran latar ikon (durasi 200ms)
                val circleBgColor by animateColorAsState(
                    targetValue = if (isSelected) NavActiveCircle else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "circleBgColor"
                )

                // Animasi transisi warna ikon (durasi 200ms)
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) TextPrimary else TextSecondary,
                    animationSpec = tween(durationMillis = 200),
                    label = "iconColor"
                )

                // Animasi transisi warna label teks (durasi 200ms)
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) TextPrimary else TextSecondary,
                    animationSpec = tween(durationMillis = 200),
                    label = "textColor"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemSelected(screen) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Indikator lingkaran abu di belakang ikon saat aktif
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(circleBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        screen.icon?.let { iconVector ->
                            Icon(
                                imageVector = iconVector,
                                contentDescription = stringResource(id = screen.titleResId),
                                tint = iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Teks label menu
                    Text(
                        text = stringResource(id = screen.titleResId),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}
