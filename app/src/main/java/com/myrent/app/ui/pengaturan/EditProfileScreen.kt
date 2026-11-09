package com.myrent.app.ui.pengaturan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.myrent.app.R
import com.myrent.app.ui.theme.AccentGold
import com.myrent.app.ui.theme.DarkBackground
import com.myrent.app.ui.theme.DarkSurface
import com.myrent.app.ui.theme.DarkSurfaceVariant
import com.myrent.app.ui.theme.TextPrimary
import com.myrent.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Layar Penuh Edit Profil Pemilik, Toko, & Email
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userDoc by viewModel.userDocState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is EditProfileUiState.Loading

    var namaPemilik by remember(userDoc) { mutableStateOf(userDoc.nama) }
    var namaToko by remember(userDoc) { mutableStateOf(userDoc.namaToko) }
    var noWaToko by remember(userDoc) { mutableStateOf(userDoc.noWaToko) }
    var alamatToko by remember(userDoc) { mutableStateOf(userDoc.alamatToko) }
    var emailInput by remember(userDoc) { mutableStateOf(userDoc.email) }
    var fotoBase64 by remember(userDoc) { mutableStateOf(userDoc.fotoProfilBase64) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    var showPhotoOptionsSheet by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showCameraRationaleDialog by remember { mutableStateOf(false) }

    val fotoRepo = viewModel.getFotoRepository()
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState()

    val hasUnsavedChanges = remember(namaPemilik, namaToko, noWaToko, alamatToko, emailInput, fotoBase64, selectedImageUri) {
        namaPemilik != userDoc.nama ||
                namaToko != userDoc.namaToko ||
                noWaToko != userDoc.noWaToko ||
                alamatToko != userDoc.alamatToko ||
                emailInput != userDoc.email ||
                fotoBase64 != userDoc.fotoProfilBase64 ||
                selectedImageUri != null
    }

    val handleBackPress = {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    BackHandler { handleBackPress() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val compressed = viewModel.compressLogoUriToBase64(context, uri)
                if (compressed != null) {
                    fotoBase64 = compressed
                    selectedImageUri = uri
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            coroutineScope.launch {
                val compressed = viewModel.compressLogoUriToBase64(context, tempCameraUri!!)
                if (compressed != null) {
                    fotoBase64 = compressed
                    selectedImageUri = tempCameraUri
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val file = viewModel.createTempCameraFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            showCameraRationaleDialog = true
        }
    }

    val launchCamera = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val file = viewModel.createTempCameraFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profil Toko", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { handleBackPress() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState is EditProfileUiState.Error) {
                Text(
                    text = (uiState as EditProfileUiState.Error).message,
                    color = Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            } else if (uiState is EditProfileUiState.Success) {
                Text(
                    text = (uiState as EditProfileUiState.Success).message,
                    color = Color(0xFF6BCB77),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Avatar Foto Profil / Logo Toko Bulat
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clickable { showPhotoOptionsSheet = true },
                contentAlignment = Alignment.BottomEnd
            ) {
                val bitmapFromBase64 = remember(fotoBase64) {
                    if (fotoBase64.isNotEmpty()) fotoRepo.base64ToBitmap(fotoBase64) else null
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmapFromBase64 != null) {
                        Image(
                            bitmap = bitmapFromBase64.asImageBitmap(),
                            contentDescription = "Logo Toko",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Store,
                            contentDescription = "Logo Toko Default",
                            tint = AccentGold,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AccentGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Ganti Foto",
                        tint = DarkBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ketuk foto untuk mengunggah logo toko",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nama Pemilik (Wajib)
            OutlinedTextField(
                value = namaPemilik,
                onValueChange = {
                    namaPemilik = it
                    viewModel.resetUiState()
                },
                label = { Text(stringResource(R.string.label_owner_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Nama Toko / Cabang (Wajib)
            OutlinedTextField(
                value = namaToko,
                onValueChange = {
                    namaToko = it
                    viewModel.resetUiState()
                },
                label = { Text(stringResource(R.string.label_store_name)) },
                placeholder = { Text(stringResource(R.string.placeholder_store_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Email Akun (Wajib)
            OutlinedTextField(
                value = emailInput,
                onValueChange = {
                    emailInput = it
                    viewModel.resetUiState()
                },
                label = { Text("Email Akun Toko") },
                placeholder = { Text("nama@email.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Nomor WA Toko (Opsional / 10-14 digit)
            OutlinedTextField(
                value = noWaToko,
                onValueChange = { input ->
                    noWaToko = input.filter { it.isDigit() || it == '+' }
                    viewModel.resetUiState()
                },
                label = { Text("Nomor WhatsApp Toko") },
                placeholder = { Text("08123456789") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Alamat Toko (Opsional / Max 200 Karakter)
            OutlinedTextField(
                value = alamatToko,
                onValueChange = { input ->
                    if (input.length <= 200) {
                        alamatToko = input
                        viewModel.resetUiState()
                    }
                },
                label = { Text("Alamat Toko (opsional)") },
                placeholder = { Text("Jl. Janti-Yogyakarta No. 123") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("${alamatToko.length}/200", color = TextSecondary, fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = AccentGold, unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tombol Simpan (Tinggi 50dp)
            Button(
                onClick = {
                    viewModel.saveProfile(
                        namaPemilik = namaPemilik,
                        namaToko = namaToko,
                        noWaTokoRaw = noWaToko,
                        alamatToko = alamatToko,
                        emailInput = emailInput,
                        fotoBase64 = fotoBase64,
                        onSuccess = onBack
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
                    Text("Simpan Perubahan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showPhotoOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoOptionsSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("Foto Logo Toko", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPhotoOptionsSheet = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Photo, contentDescription = "Galeri", tint = AccentGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Ambil dari Galeri", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPhotoOptionsSheet = false
                            launchCamera()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = "Kamera", tint = AccentGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Ambil Foto dengan Kamera", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                if (fotoBase64.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoOptionsSheet = false
                                fotoBase64 = ""
                                selectedImageUri = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Hapus", tint = Color(0xFFFF6B6B), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Hapus Foto", color = Color(0xFFFF6B6B), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Batalkan Perubahan?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Anda memiliki perubahan profil yang belum disimpan. Yakin ingin kembali?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onBack()
                }) {
                    Text("Ya, Batalkan", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Lanjut Edit", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showCameraRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showCameraRationaleDialog = false },
            title = { Text("Izin Kamera Dibutuhkan", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Aplikasi membutuhkan izin kamera untuk mengambil foto logo toko secara langsung.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showCameraRationaleDialog = false }) {
                    Text("Mengerti", color = AccentGold, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface
        )
    }
}
