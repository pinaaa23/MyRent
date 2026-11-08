package com.myrent.app.ui.pengaturan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.R
import com.myrent.app.data.kategori.Kategori
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Dialog Kelola Kategori Barang
 */
@Composable
fun KelolaKategoriDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val kategoriList by viewModel.kategoriList.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var kategoriToEdit by remember { mutableStateOf<Kategori?>(null) }
    var kategoriToDelete by remember { mutableStateOf<Kategori?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_manage_category), color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (uiState is SettingsUiState.Error) {
                    Text(
                        text = (uiState as SettingsUiState.Error).message,
                        color = Color(0xFFFF6B6B),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        kategoriToEdit = null
                        viewModel.resetUiState()
                        showFormDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkBackground)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Tambah")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_add_category), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(kategoriList) { kat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Category,
                                    contentDescription = "Kategori",
                                    tint = AccentGold,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(kat.nama, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    kategoriToEdit = kat
                                    viewModel.resetUiState()
                                    showFormDialog = true
                                }) {
                                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit", tint = AccentGold)
                                }
                                IconButton(onClick = {
                                    kategoriToDelete = kat
                                    viewModel.resetUiState()
                                }) {
                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Hapus", tint = Color(0xFFFF6B6B))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = AccentGold)
            }
        },
        containerColor = DarkSurface
    )

    // Form Tambah/Edit Kategori Sub-Dialog
    if (showFormDialog) {
        FormKategoriSubDialog(
            kategori = kategoriToEdit,
            viewModel = viewModel,
            onDismiss = { showFormDialog = false }
        )
    }

    // Dialog Konfirmasi Hapus Kategori
    if (kategoriToDelete != null) {
        AlertDialog(
            onDismissRequest = { kategoriToDelete = null },
            title = { Text(stringResource(R.string.confirm_delete_category_title), color = TextPrimary) },
            text = {
                Text(
                    text = stringResource(R.string.confirm_delete_category_msg, kategoriToDelete!!.nama),
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = kategoriToDelete!!
                    kategoriToDelete = null
                    viewModel.hapusKategori(target, onSuccess = {})
                }) {
                    Text(stringResource(R.string.btn_delete), color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { kategoriToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun FormKategoriSubDialog(
    kategori: Kategori?,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    var nama by remember { mutableStateOf(kategori?.nama ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (kategori != null) stringResource(R.string.title_edit_category) else stringResource(R.string.title_add_category),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                label = { Text(stringResource(R.string.label_category_name)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (kategori == null) {
                    viewModel.tambahKategori(nama, onSuccess = onDismiss)
                } else {
                    viewModel.editKategori(kategori.id, nama, onSuccess = onDismiss)
                }
            }) {
                Text(stringResource(R.string.btn_save), color = AccentGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
