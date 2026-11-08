package com.myrent.app.ui.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
 * Layar Pendaftaran (Buat Akun Baru)
 */
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedCabang by remember { mutableStateOf<Cabang?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isCabangDropdownExpanded by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val daftarCabang by viewModel.daftarCabangState.collectAsState()
    val isLoadingCabang by viewModel.isLoadingCabang.collectAsState()
    val isLoading = uiState is AuthUiState.Loading

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.title_register),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Pesan Error jika ada
        if (uiState is AuthUiState.Error) {
            val errorRes = (uiState as AuthUiState.Error).messageResId
            Text(
                text = stringResource(id = errorRes),
                color = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Input Nama Lengkap
        OutlinedTextField(
            value = nama,
            onValueChange = {
                nama = it
                viewModel.resetUiState()
            },
            label = { Text(stringResource(id = R.string.label_full_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGold,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedLabelColor = AccentGold,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Input Email
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                viewModel.resetUiState()
            },
            label = { Text(stringResource(id = R.string.label_email)) },
            placeholder = { Text(stringResource(id = R.string.placeholder_email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGold,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedLabelColor = AccentGold,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Input Kata Sandi
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                viewModel.resetUiState()
            },
            label = { Text(stringResource(id = R.string.label_password)) },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = TextSecondary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGold,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedLabelColor = AccentGold,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Input Ulangi Kata Sandi
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                viewModel.resetUiState()
            },
            label = { Text(stringResource(id = R.string.label_confirm_password)) },
            singleLine = true,
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                    Icon(
                        imageVector = if (isConfirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle confirm password visibility",
                        tint = TextSecondary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGold,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedLabelColor = AccentGold,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Pilihan Cabang (Dropdown Menu)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedCabang?.nama ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(id = R.string.label_select_cabang)) },
                trailingIcon = {
                    if (isLoadingCabang) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AccentGold,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Dropdown Cabang",
                            tint = TextSecondary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isCabangDropdownExpanded = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = DarkSurfaceVariant,
                    disabledLabelColor = TextSecondary,
                    disabledTextColor = TextPrimary,
                    disabledTrailingIconColor = TextSecondary
                )
            )

            // Transparent overlay overlaying the field to capture click
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        if (!isLoadingCabang) isCabangDropdownExpanded = true
                    }
            )

            DropdownMenu(
                expanded = isCabangDropdownExpanded,
                onDismissRequest = { isCabangDropdownExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(DarkSurface)
            ) {
                if (daftarCabang.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(id = R.string.empty_cabang_warning),
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        onClick = { isCabangDropdownExpanded = false }
                    )
                } else {
                    daftarCabang.forEach { cabang ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = cabang.nama,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (cabang.alamat.isNotEmpty()) {
                                        Text(
                                            text = cabang.alamat,
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedCabang = cabang
                                isCabangDropdownExpanded = false
                                viewModel.resetUiState()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Daftar
        Button(
            onClick = {
                viewModel.register(
                    nama = nama,
                    email = email,
                    password = password,
                    confirmPassword = confirmPassword,
                    selectedCabang = selectedCabang,
                    onSuccess = onRegisterSuccess
                )
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = DarkBackground,
                disabledContainerColor = DarkSurfaceVariant,
                disabledContentColor = TextSecondary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = DarkBackground,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(id = R.string.btn_register),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tautan Ke Layar Login
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.text_already_have_account),
                color = TextSecondary,
                fontSize = 14.sp
            )
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = stringResource(id = R.string.link_login),
                    color = AccentGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
