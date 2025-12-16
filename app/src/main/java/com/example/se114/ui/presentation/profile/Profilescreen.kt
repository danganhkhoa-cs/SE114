package com.example.se114.ui.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.local.PreferencesManager
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    preferencesManager: PreferencesManager,
    onNavigateToAccountSettings: () -> Unit = {},
    onNavigateToAccountData: () -> Unit = {},
    onNavigateToHelpSupport: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentLanguage = preferencesManager.languageState.value
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    // Load Data
    LaunchedEffect(Unit) {
        viewModel.setInitialData(preferencesManager.userName, preferencesManager.userBio)
    }

    // --- LẮNG NGHE SỰ KIỆN LOGOUT THÀNH CÔNG ---
    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) {
            // 1. Xóa dữ liệu local (nếu cần thiết, ví dụ clear token)
            // preferencesManager.clearUserData() // Nếu bạn có hàm này

            // 2. Điều hướng ra ngoài
            onLogout()

            // 3. Reset state viewModel
            viewModel.onLogoutHandled()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp)
                        .padding(top = 50.dp, bottom = 30.dp), // Thêm bottom padding để khi scroll xuống cuối không bị sát mép
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = preferencesManager.getString("profile_title"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // Avatar Section
                    Surface(
                        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(70.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape).padding(3.dp)) {
                                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Person, "Avatar", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(uiState.userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(uiState.userBio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                            IconButton(
                                onClick = viewModel::showEditProfileDialog,
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Menu Section
                    Surface(
                        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            ProfileMenuItem(Icons.Outlined.Person, preferencesManager.getString("account_settings"), onNavigateToAccountSettings)
                            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ProfileMenuItem(Icons.Outlined.Edit, preferencesManager.getString("account_data"), onNavigateToAccountData)
                            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ProfileMenuItem(Icons.Outlined.Info, preferencesManager.getString("help_support"), onNavigateToHelpSupport)
                            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ProfileMenuItem(Icons.Outlined.Settings, preferencesManager.getString("settings"), onNavigateToSettings)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Logout Button (Trigger Dialog)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .clickable { viewModel.showLogoutDialog() },
                        shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(preferencesManager.getString("logout"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Navigate", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }

        // --- Loading Overlay (Khi đang logout) ---
        if (uiState.isLoggingOut) {
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

    // --- Dialogs ---

    if (uiState.isShowingEditProfileDialog) {
        EditProfileDialog(
            currentName = uiState.userName,
            currentBio = uiState.userBio,
            onDismiss = viewModel::hideEditProfileDialog,
            onSave = { name, bio ->
                preferencesManager.userName = name
                preferencesManager.userBio = bio
                viewModel.updateUserInfo(name, bio)
            },
            preferencesManager = preferencesManager,
            onChangeAvatar = {
                scope.launch { snackbarHostState.showSnackbar(preferencesManager.getString("avatar_updated")) }
            }
        )
    }

    if (uiState.isShowingLogoutDialog) {
        LogoutConfirmationDialog(
            onDismiss = viewModel::hideLogoutDialog,
            onConfirm = {
                // Gọi ViewModel để xử lý Logout (kết nối server)
                viewModel.logout()
            },
            preferencesManager = preferencesManager
        )
    }
}

// --- SUB COMPOSABLES (Giữ nguyên) ---

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, color = Color.Transparent) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, title, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Navigate", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun EditProfileDialog(
    currentName: String, currentBio: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit,
    preferencesManager: PreferencesManager, onChangeAvatar: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)), CircleShape).padding(3.dp)) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface, CircleShape).padding(2.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, "Avatar", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
                        }
                    }
                }
                TextButton(onClick = onChangeAvatar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(preferencesManager.getString("change_avatar"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(preferencesManager.getString("edit_profile"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(value = name, onValueChange = { name = it; errorMessage = "" }, label = { Text(preferencesManager.getString("name")) }, leadingIcon = { Icon(Icons.Filled.Person, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text(preferencesManager.getString("bio")) }, leadingIcon = { Icon(Icons.Filled.Edit, null) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3)
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
                        Text(preferencesManager.getString("cancel"), color = MaterialTheme.colorScheme.primary)
                    }
                    Button(onClick = { if (name.isEmpty()) errorMessage = preferencesManager.getString("name_empty_error") else onSave(name, bio) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                        Text(preferencesManager.getString("save"))
                    }
                }
            }
        }
    }
}

@Composable
fun LogoutConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, preferencesManager: PreferencesManager) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(preferencesManager.getString("logout_confirm_title"), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) },
        text = { Text(preferencesManager.getString("logout_confirm_msg"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(preferencesManager.getString("yes"), color = MaterialTheme.colorScheme.onError) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(preferencesManager.getString("no"), color = MaterialTheme.colorScheme.onSurface) } },
        containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)
    )
}