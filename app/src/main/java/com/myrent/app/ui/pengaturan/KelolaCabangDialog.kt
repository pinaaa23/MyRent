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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Store
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
import com.myrent.app.data.cabang.Cabang
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Dialog Kelola Cabang Toko
 */
@Composable
fun KelolaCabangDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val cabangList by viewModel.cabangList.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var cabangToEdit by remember { mutableStateOf<Cabang?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_manage_branch), color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                        cabangToEdit = null
                        viewModel.resetUiState()
                        showFormDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkBackground)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Tambah")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_add_branch), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(cabangList) { cabang ->
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
                                    imageVector = Icons.Filled.Store,
                                    contentDescription = "Cabang",
                                    tint = AccentGold,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cabang.nama, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    if (cabang.alamat.isNotEmpty()) {
                                        Text(cabang.alamat, color = TextSecondary, fontSize = 12.sp)
                                    }
                                    if (cabang.noWa.isNotEmpty()) {
                                        Text(cabang.noWa, color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                                IconButton(onClick = {
                                    cabangToEdit = cabang
                                    viewModel.resetUiState()
                                    showFormDialog = true
                                }) {
                                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit", tint = AccentGold)
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

    // Sub-Dialog Form Tambah / Edit Cabang
    if (showFormDialog) {
        FormCabangSubDialog(
            cabang = cabangToEdit,
            viewModel = viewModel,
            onDismiss = { showFormDialog = false }
        )
    }
}

@Composable
fun FormCabangSubDialog(
    cabang: Cabang?,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    var nama by remember { mutableStateOf(cabang?.nama ?: "") }
    var alamat by remember { mutableStateOf(cabang?.alamat ?: "") }
    var noWa by remember { mutableStateOf(cabang?.noWa ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (cabang != null) stringResource(R.string.title_edit_branch) else stringResource(R.string.title_add_branch),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text(stringResource(R.string.label_branch_name)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = { Text(stringResource(R.string.label_branch_address)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = noWa,
                    onValueChange = { noWa = it },
                    label = { Text(stringResource(R.string.label_branch_phone)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (cabang == null) {
                    viewModel.tambahCabang(nama, alamat, noWa, onSuccess = onDismiss)
                } else {
                    viewModel.updateCabang(cabang.id, nama, alamat, noWa, onSuccess = onDismiss)
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
