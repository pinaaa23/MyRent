package com.myrent.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground

/**
 * Komponen Latar Belakang Komunal Mewah dengan Warna Solid #171513 & Radial Ambient Gold Glow di Pojok
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Latar Belakang Warna Solid #171513 (Ganti dari Brush.verticalGradient)
            drawRect(
                color = DarkBackground,
                size = size
            )

            // 2. Top-Right Radial Ambient Gold Glow (Alpha ~13% memudar ke Transparan - Elemen Dekorasi)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(AccentGold.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(w * 0.95f, h * 0.05f),
                    radius = w * 0.85f
                ),
                size = size
            )

            // 3. Bottom-Left Radial Ambient Gold Glow (Alpha ~11% memudar ke Transparan - Elemen Dekorasi)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(AccentGold.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(w * 0.05f, h * 0.95f),
                    radius = w * 0.85f
                ),
                size = size
            )
        }

        content()
    }
}
