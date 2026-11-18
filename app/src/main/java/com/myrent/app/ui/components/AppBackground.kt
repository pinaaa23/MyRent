package com.myrent.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.myrent.app.ui.theme.DarkBackground

/**
 * Komponen Latar Belakang Komunal dengan 1 Warna Solid Murni (#171513) Tanpa Gradasi / Siluet
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        content()
    }
}
