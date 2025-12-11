package com.example.se114.ui.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.local.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    preferencesManager: PreferencesManager,
    onBackClick: () -> Unit,
    viewModel: AccountSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Force recomposition khi đổi ngôn ngữ
    val currentLanguage = preferencesManager.languageState.value

    // Load initial data
    LaunchedEffect(Unit) {
        viewModel.setInitialData(
            email = preferencesManager.userEmail,
            phone = preferencesManager.userPhone
        )
    }

    // Masking logic (UI only)
    val maskedEmail = maskEmail(uiState.email)
    val maskedPhone = maskPhone(uiState.phone)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        preferencesManager.getString("account_settings"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingItemCard(
                icon = Icons.Default.Email,
                title = preferencesManager.getString("email"),
                value = maskedEmail,
                isEditable = false
            )
            SettingItemCard(
                icon = Icons.Default.Lock,
                title = preferencesManager.getString("password"),
                value = "••••••••",
                isEditable = true,
                onEditClick = viewModel::showPasswordDialog
            )
            SettingItemCard(
                icon = Icons.Default.Phone,
                title = preferencesManager.getString("phone_number"),
                value = maskedPhone,
                isEditable = true,
                onEditClick = viewModel::showPasswordVerifyDialog
            )
        }
    }

    // --- Dialogs ---

    if (uiState.isShowingPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = viewModel::hidePasswordDialog,
            onConfirm = { _, _ ->
                // Logic đổi mật khẩu thực tế sẽ nằm ở đây (gọi API hoặc lưu Prefs)
                viewModel.hidePasswordDialog()
            },
            preferencesManager = preferencesManager
        )
    }

    if (uiState.isShowingPasswordVerifyDialog) {
        VerifyPasswordDialog(
            onDismiss = viewModel::hidePasswordVerifyDialog,
            onVerified = {
                // Xác thực thành công -> Mở dialog đổi số điện thoại
                viewModel.hidePasswordVerifyDialog()
                viewModel.showPhoneDialog()
            },
            preferencesManager = preferencesManager
        )
    }

    if (uiState.isShowingPhoneDialog) {
        ChangePhoneDialog(
            currentPhone = uiState.phone,
            onDismiss = viewModel::hidePhoneDialog,
            onConfirm = { newPhone ->
                // 1. Lưu vào Preferences
                preferencesManager.userPhone = newPhone
                // 2. Cập nhật UI State
                viewModel.updatePhone(newPhone)
            },
            preferencesManager = preferencesManager
        )
    }
}

// --- HELPER FUNCTIONS & COMPOSABLES (Giữ nguyên) ---

@Composable
fun SettingItemCard(
    icon: ImageVector,
    title: String,
    value: String,
    isEditable: Boolean,
    onEditClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (isEditable) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    preferencesManager: PreferencesManager
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = preferencesManager.getString("change_password"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text(preferencesManager.getString("current_password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(preferencesManager.getString("new_password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(preferencesManager.getString("confirm_password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(preferencesManager.getString("cancel"), color = MaterialTheme.colorScheme.primary)
                    }

                    Button(
                        onClick = {
                            when {
                                oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                                    errorMessage = preferencesManager.getString("field_empty_error")
                                }
                                newPassword != confirmPassword -> {
                                    errorMessage = preferencesManager.getString("passwords_not_match")
                                }
                                newPassword.length < 6 -> {
                                    errorMessage = preferencesManager.getString("password_length_error")
                                }
                                else -> {
                                    onConfirm(oldPassword, newPassword)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(preferencesManager.getString("change"))
                    }
                }
            }
        }
    }
}

@Composable
fun VerifyPasswordDialog(
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
    preferencesManager: PreferencesManager
) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = preferencesManager.getString("verify_password"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = preferencesManager.getString("verify_password_msg"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(preferencesManager.getString("password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(preferencesManager.getString("cancel"), color = MaterialTheme.colorScheme.primary)
                    }

                    Button(
                        onClick = {
                            if (password.isEmpty()) {
                                errorMessage = preferencesManager.getString("password_required")
                            } else {
                                onVerified()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(preferencesManager.getString("verify"))
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePhoneDialog(
    currentPhone: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    preferencesManager: PreferencesManager
) {
    var newPhone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = preferencesManager.getString("change_phone"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${preferencesManager.getString("current")}: ${maskPhone(currentPhone)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it },
                    label = { Text(preferencesManager.getString("new_phone")) },
                    placeholder = { Text("0123456789") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(preferencesManager.getString("cancel"), color = MaterialTheme.colorScheme.primary)
                    }

                    Button(
                        onClick = {
                            when {
                                newPhone.isEmpty() -> {
                                    errorMessage = preferencesManager.getString("phone_required")
                                }
                                !newPhone.matches(Regex("^0\\d{9}$")) -> {
                                    errorMessage = preferencesManager.getString("invalid_phone")
                                }
                                else -> {
                                    onConfirm(newPhone)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(preferencesManager.getString("confirm"))
                    }
                }
            }
        }
    }
}

fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return email

    val username = parts[0]
    val domain = parts[1]

    return if (username.length <= 2) {
        email
    } else {
        val firstChar = username.first()
        val lastChar = username.last()
        val maskedLength = username.length - 2
        "$firstChar${"*".repeat(maskedLength)}$lastChar@$domain"
    }
}

fun maskPhone(phone: String): String {
    return if (phone.length >= 3) {
        "*".repeat(phone.length - 3) + phone.takeLast(3)
    } else {
        phone
    }
}