package com.example.se114.ui.presentation.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
    // ViewModel sẽ được Inject tự động bởi Hilt
    // Nếu vẫn lỗi "Unresolved reference", hãy kiểm tra kỹ file AccountSettingsViewModel.kt có đúng package không
    viewModel: AccountSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Refresh data khi mở màn hình
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    // Xử lý thông báo Toast
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Email (Read-only)
                AccountInfoItem(
                    label = preferencesManager.getString("email"),
                    value = maskEmail(uiState.email),
                    icon = Icons.Default.Email,
                    isEditable = false
                )

                // Phone (Editable)
                AccountInfoItem(
                    label = preferencesManager.getString("phone_number"),
                    value = maskPhone(uiState.phone),
                    icon = Icons.Default.Phone,
                    isEditable = true,
                    onEditClick = { viewModel.showPasswordVerifyDialog() }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Change Password Button
                Button(
                    onClick = { viewModel.showPasswordDialog() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        preferencesManager.getString("change_password"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Loading Indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Change Password Dialog
    if (uiState.isShowingPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { viewModel.hidePasswordDialog() },
            onConfirm = { current, new -> viewModel.changePassword(current, new) },
            preferencesManager = preferencesManager
        )
    }

    // 2. Verify Password Dialog (Before changing phone)
    if (uiState.isShowingPasswordVerifyDialog) {
        PasswordVerificationDialog(
            onDismiss = { viewModel.hidePasswordVerifyDialog() },
            onConfirm = { pass -> viewModel.verifyPasswordForPhoneChange(pass) },
            preferencesManager = preferencesManager
        )
    }

    // 3. Change Phone Dialog (After verification)
    if (uiState.isShowingPhoneDialog) {
        ChangePhoneDialog(
            onDismiss = { viewModel.hidePhoneDialog() },
            onConfirm = { newPhone -> viewModel.updatePhone(newPhone) },
            preferencesManager = preferencesManager
        )
    }
}

// --- SUB COMPONENTS ---

@Composable
fun AccountInfoItem(
    label: String,
    value: String,
    icon: ImageVector,
    isEditable: Boolean = false,
    onEditClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
            if (isEditable) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// --- DIALOGS IMPLEMENTATION ---

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    preferencesManager: PreferencesManager
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(preferencesManager.getString("change_password"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(preferencesManager.getString("current_password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(preferencesManager.getString("new_password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(preferencesManager.getString("confirm_password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text(preferencesManager.getString("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (newPassword.length < 6) {
                            error = preferencesManager.getString("password_not_strong")
                        } else if (newPassword != confirmPassword) {
                            error = preferencesManager.getString("passwords_not_match")
                        } else {
                            onConfirm(currentPassword, newPassword)
                        }
                    }) {
                        Text(preferencesManager.getString("save"))
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordVerificationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    preferencesManager: PreferencesManager
) {
    var password by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(preferencesManager.getString("verify_password"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(preferencesManager.getString("verify_password_msg"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(preferencesManager.getString("password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text(preferencesManager.getString("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(password) }) {
                        Text(preferencesManager.getString("confirm"))
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePhoneDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    preferencesManager: PreferencesManager
) {
    var newPhone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val phoneRegex = Regex("^(0|\\+84)[35789]\\d{8}$")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(preferencesManager.getString("change_phone"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it },
                    label = { Text(preferencesManager.getString("new_phone")) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text(preferencesManager.getString("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (newPhone.isBlank()) {
                            error = preferencesManager.getString("phone_required")
                        } else if (!newPhone.matches(phoneRegex)) {
                            error = preferencesManager.getString("invalid_phone")
                        } else {
                            onConfirm(newPhone)
                        }
                    }) {
                        Text(preferencesManager.getString("save"))
                    }
                }
            }
        }
    }
}

// Helper functions for masking
fun maskEmail(email: String): String {
    if (email.isBlank()) return ""
    val parts = email.split("@")
    if (parts.size != 2) return email
    val name = parts[0]
    val domain = parts[1]
    if (name.length <= 2) return "$name***@$domain"
    return "${name.first()}***${name.last()}@$domain"
}

fun maskPhone(phone: String): String {
    if (phone.length < 4) return phone
    return "*******${phone.takeLast(3)}"
}