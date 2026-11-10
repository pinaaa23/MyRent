package com.myrent.app.ui.katalog

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myrent.app.R
import com.myrent.app.data.items.Item
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary

/**
 * Bottom Sheet Formulir Tambah dan Edit Barang dengan Input Stok & Tambah Kategori Inline
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemBottomSheet(
    sheetState: SheetState,
    viewModel: CatalogViewModel,
    itemToEdit: Item?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = itemToEdit != null

    var nama by remember { mutableStateOf(itemToEdit?.nama ?: "") }
    var selectedKategoriNama by remember { mutableStateOf(itemToEdit?.kategori ?: "") }
    var ukuran by remember { mutableStateOf(itemToEdit?.ukuran ?: "") }
    var stokText by remember { mutableStateOf(if (isEditMode) itemToEdit?.stok.toString() else "1") }
    var hargaSewaText by remember { mutableStateOf(if (isEditMode) itemToEdit?.hargaSewa.toString() else "") }
    var catatan by remember { mutableStateOf(itemToEdit?.catatan ?: "") }
    var status by remember { mutableStateOf(itemToEdit?.status ?: "tersedia") }

    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isKategoriDropdownExpanded by remember { mutableStateOf(false) }
    var isStatusDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddCategoryInlineDialog by remember { mutableStateOf(false) }
    var newCategoryInlineInput by remember { mutableStateOf("") }

    val kategoriList by viewModel.kategoriList.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is CatalogUiState.Loading

    // Auto-select kategori pertama jika belum ada kategori terpilih
    LaunchedEffect(kategoriList) {
        if (selectedKategoriNama.isEmpty() && kategoriList.isNotEmpty()) {
            selectedKategoriNama = kategoriList.first().nama
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEditMode) stringResource(R.string.title_edit_item) else stringResource(R.string.title_add_item),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState is CatalogUiState.Error) {
                Text(
                    text = (uiState as CatalogUiState.Error).message,
                    color = Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Pratinjau Foto & Tombol Photo Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val fotoRepo = viewModel.getFotoRepository()
                val bitmapFromBase64 = remember(itemToEdit?.fotoBase64) {
                    if (itemToEdit?.fotoBase64?.isNotEmpty() == true) {
                        fotoRepo.base64ToBitmap(itemToEdit.fotoBase64)
                    } else null
                }

                if (selectedPhotoUri != null) {
                    AsyncImageUriPreview(uri = selectedPhotoUri!!)
                } else if (bitmapFromBase64 != null) {
                    Image(
                        bitmap = bitmapFromBase64.asImageBitmap(),
                        contentDescription = "Foto Barang",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = "Pilih Foto",
                            tint = AccentGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.btn_select_photo),
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nama Barang
            OutlinedTextField(
                value = nama,
                onValueChange = {
                    nama = it
                    viewModel.resetUiState()
                },
                label = { Text(stringResource(R.string.label_item_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Kategori (Dropdown + Fitur Tambah Kategori Langsung)
            Box(modifier = Modifier.fillMaxWidth()) {
                val displayKategoriText = selectedKategoriNama.ifEmpty {
                    if (kategoriList.isEmpty()) "Belum ada kategori (Ketuk untuk tambah)" else "Pilih Kategori"
                }

                OutlinedTextField(
                    value = displayKategoriText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_item_category)) },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown Kategori", tint = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            if (kategoriList.isEmpty()) {
                                showAddCategoryInlineDialog = true
                            } else {
                                isKategoriDropdownExpanded = true
                            }
                        }
                )

                DropdownMenu(
                    expanded = isKategoriDropdownExpanded,
                    onDismissRequest = { isKategoriDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(DarkSurface)
                ) {
                    kategoriList.forEach { kat ->
                        DropdownMenuItem(
                            text = { Text(text = kat.nama, color = TextPrimary) },
                            onClick = {
                                selectedKategoriNama = kat.nama
                                isKategoriDropdownExpanded = false
                                viewModel.resetUiState()
                            }
                        )
                    }

                    // Opsi Tambah Kategori Baru di dalam Dropdown
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = "Tambah", tint = AccentGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "+ Tambah Kategori Baru", color = AccentGold, fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = {
                            isKategoriDropdownExpanded = false
                            showAddCategoryInlineDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ukuran (Opsional) & Jumlah Stok
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = ukuran,
                    onValueChange = { ukuran = it },
                    label = { Text(stringResource(R.string.label_item_size)) },
                    placeholder = { Text("M, L, XL") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )

                // Input Jumlah Stok Barang
                OutlinedTextField(
                    value = stokText,
                    onValueChange = { input ->
                        stokText = input.filter { it.isDigit() }
                        viewModel.resetUiState()
                    },
                    label = { Text(stringResource(R.string.label_item_stock)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Harga Sewa (Hanya Angka)
            OutlinedTextField(
                value = hargaSewaText,
                onValueChange = { input ->
                    hargaSewaText = input.filter { it.isDigit() }
                    viewModel.resetUiState()
                },
                label = { Text(stringResource(R.string.label_item_price)) },
                placeholder = { Text("150000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status Barang
            if (status == "disewa") {
                OutlinedTextField(
                    value = stringResource(R.string.rented_status_warning),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text(stringResource(R.string.label_item_status)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = DarkSurfaceVariant,
                        disabledLabelColor = AccentGold,
                        disabledTextColor = AccentGold
                    )
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (status == "perbaikan") stringResource(R.string.status_maintenance) else stringResource(R.string.status_available),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_item_status)) },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown Status", tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                            focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { isStatusDropdownExpanded = true }
                    )

                    DropdownMenu(
                        expanded = isStatusDropdownExpanded,
                        onDismissRequest = { isStatusDropdownExpanded = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.status_available), color = TextPrimary) },
                            onClick = {
                                status = "tersedia"
                                isStatusDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.status_maintenance), color = TextPrimary) },
                            onClick = {
                                status = "perbaikan"
                                isStatusDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Jaminan / Syarat Sewa
            OutlinedTextField(
                value = catatan,
                onValueChange = { catatan = it },
                label = { Text("Jaminan / Syarat Sewa (opsional)") },
                placeholder = { Text("misal: KTP / SIM / Kartu Pelajar") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Simpan
            Button(
                onClick = {
                    val hargaLong = hargaSewaText.toLongOrNull() ?: 0L
                    val stokInt = stokText.toIntOrNull() ?: 1
                    viewModel.simpanItem(
                        id = itemToEdit?.id ?: "",
                        nama = nama,
                        kategori = selectedKategoriNama,
                        ukuran = ukuran,
                        stok = stokInt,
                        hargaSewa = hargaLong,
                        fotoBase64Existing = itemToEdit?.fotoBase64 ?: "",
                        newPhotoUri = selectedPhotoUri,
                        status = status,
                        catatan = catatan,
                        context = context,
                        onSuccess = onDismiss
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold, contentColor = DarkBackground,
                    disabledContainerColor = DarkSurfaceVariant, disabledContentColor = TextSecondary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DarkBackground, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = stringResource(R.string.btn_save),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tombol Hapus saat mode edit
            if (isEditMode) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        if (status == "disewa") {
                            viewModel.simpanItem(
                                id = itemToEdit?.id ?: "",
                                nama = nama,
                                kategori = selectedKategoriNama,
                                ukuran = ukuran,
                                stok = stokText.toIntOrNull() ?: 1,
                                hargaSewa = hargaSewaText.toLongOrNull() ?: 0L,
                                fotoBase64Existing = itemToEdit?.fotoBase64 ?: "",
                                newPhotoUri = null,
                                status = status,
                                catatan = catatan,
                                context = context,
                                onSuccess = {}
                            )
                        } else {
                            showDeleteConfirmDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B))
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Hapus", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.btn_delete), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialog Tambah Kategori Langsung (Inline)
    if (showAddCategoryInlineDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryInlineDialog = false },
            title = { Text("Tambah Kategori Baru", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Masukkan nama kategori barang baru yang ingin ditambahkan:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCategoryInlineInput,
                        onValueChange = { newCategoryInlineInput = it },
                        label = { Text("Nama Kategori Baru") },
                        placeholder = { Text("misal: Kamera, Gaun, Camp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                            focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = newCategoryInlineInput.trim()
                        if (input.isNotEmpty()) {
                            viewModel.tambahKategoriInline(input) { addedCat ->
                                selectedKategoriNama = addedCat
                                newCategoryInlineInput = ""
                                showAddCategoryInlineDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DarkBackground)
                ) {
                    Text("Tambah", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryInlineDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Dialog Konfirmasi Hapus Barang
    if (showDeleteConfirmDialog && itemToEdit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.confirm_delete_item_title), color = TextPrimary) },
            text = { Text(text = stringResource(R.string.confirm_delete_item_msg, itemToEdit.nama), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    viewModel.hapusItem(itemToEdit, onSuccess = onDismiss)
                }) {
                    Text(stringResource(R.string.btn_delete), color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

/**
 * Komponen pembantu untuk menampilkan preview gambar dari Local Uri (Photo Picker)
 */
@Composable
fun AsyncImageUriPreview(uri: Uri) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val b = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            b
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Foto Barang Baru",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
