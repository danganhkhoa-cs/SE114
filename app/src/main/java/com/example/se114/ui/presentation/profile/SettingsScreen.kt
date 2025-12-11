package com.example.se114.ui.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.local.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    onBackClick: () -> Unit,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Ngôn ngữ & Theme (Logic UI đơn giản nên giữ state local cho việc selection)
    var currentLanguage by remember { mutableStateOf(preferencesManager.language) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        preferencesManager.getString("settings"),
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
            // Language
            SettingItemCard(
                icon = Icons.Default.Language,
                title = preferencesManager.getString("language"),
                subtitle = currentLanguage,
                onClick = viewModel::showLanguageDialog
            )

            // Theme
            SettingItemCard(
                icon = Icons.Default.Palette,
                title = preferencesManager.getString("theme"),
                subtitle = if (isDarkTheme) preferencesManager.getString("dark_mode") else preferencesManager.getString("light_mode"),
                onClick = viewModel::showThemeDialog
            )

            // Block List
            SettingItemCard(
                icon = Icons.Default.Block,
                title = preferencesManager.getString("blocked_users"),
                subtitle = preferencesManager.getString("manage_blocked"),
                onClick = viewModel::showBlockListDialog
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Delete Account - Danger Zone
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                    .clickable { viewModel.showDeleteAccountDialog() },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DeleteForever, "Delete Account", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preferencesManager.getString("delete_account"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Permanently delete your account", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Navigate", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                }
            }
        }
    }

    // --- Dialogs ---

    if (uiState.isShowingLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onDismiss = viewModel::hideLanguageDialog,
            onConfirm = { newLanguage ->
                currentLanguage = newLanguage
                preferencesManager.language = newLanguage
                viewModel.hideLanguageDialog()
            },
            preferencesManager = preferencesManager
        )
    }

    if (uiState.isShowingThemeDialog) {
        ThemeSelectionDialog(
            isDarkTheme = isDarkTheme,
            onDismiss = viewModel::hideThemeDialog,
            onConfirm = { isDarkMode ->
                onThemeChange(isDarkMode)
                viewModel.hideThemeDialog()
            },
            preferencesManager = preferencesManager
        )
    }

    if (uiState.isShowingBlockListDialog) {
        BlockListDialog(
            blockedUsers = uiState.blockedUsers,
            onDismiss = viewModel::hideBlockListDialog,
            onUnblock = viewModel::unblockUser,
            preferencesManager = preferencesManager
        )
    }

    if (uiState.isShowingDeleteAccountDialog) {
        DeleteAccountDialog(
            step = uiState.deleteStep,
            errorMessage = uiState.deleteError,
            isDeleting = uiState.isDeleting,
            onDismiss = viewModel::hideDeleteAccountDialog,
            onNext = viewModel::onDeleteNextStep,
            onBack = viewModel::onDeletePreviousStep,
            onConfirm = { confirmText ->
                viewModel.confirmDeleteAccount(confirmText) {
                    // Logic khi xóa thành công (VD: Logout)
                    onBackClick() // Tạm thời back về
                }
            },
            preferencesManager = preferencesManager
        )
    }
}

// --- COMPOSABLES ---

@Composable
fun BlockListDialog(
    blockedUsers: List<String>, // Nhận list từ ViewModel
    onDismiss: () -> Unit,
    onUnblock: (String) -> Unit, // Callback unblock
    preferencesManager: PreferencesManager
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(preferencesManager.getString("blocked_users"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(preferencesManager.getString("manage_blocked"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (blockedUsers.isEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(preferencesManager.getString("no_blocked_users"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        blockedUsers.forEach { username ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                shadowElevation = 2.dp
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                                        Text(username.first().toString().uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(username, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onUnblock(username) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(preferencesManager.getString("unblock"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), shape = RoundedCornerShape(12.dp)) {
                    Text(preferencesManager.getString("close"))
                }
            }
        }
    }
}

@Composable
fun DeleteAccountDialog(
    step: Int,
    errorMessage: String,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onNext: (Int, String) -> Unit, // Int: currentStep, String: password input
    onBack: () -> Unit,
    onConfirm: (String) -> Unit, // String: confirm text
    preferencesManager: PreferencesManager
) {
    // Local state cho input, vì input chỉ tồn tại khi dialog mở
    var passwordInput by remember { mutableStateOf("") }
    var confirmTextInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (isDeleting) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Box(modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp)).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(preferencesManager.getString("delete_account_title"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(20.dp))

                    when (step) {
                        1 -> {
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(preferencesManager.getString("delete_warning_title"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(preferencesManager.getString("delete_warning_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                    Text(preferencesManager.getString("cancel"))
                                }
                                Button(onClick = { onNext(1, "") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                    Text(preferencesManager.getString("continue"))
                                }
                            }
                        }
                        2 -> {
                            Text(preferencesManager.getString("enter_password_continue"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text(preferencesManager.getString("password")) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.error, focusedLabelColor = MaterialTheme.colorScheme.error, cursorColor = MaterialTheme.colorScheme.error),
                                isError = errorMessage.isNotEmpty()
                            )
                            if (errorMessage.isNotEmpty()) {
                                Text(errorMessage.ifEmpty { preferencesManager.getString("password_required") }, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                                    Text(preferencesManager.getString("back"), color = MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = { onNext(2, passwordInput) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(preferencesManager.getString("next"))
                                }
                            }
                        }
                        3 -> {
                            Text(preferencesManager.getString("type_delete"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = confirmTextInput,
                                onValueChange = { confirmTextInput = it },
                                label = { Text("Type DELETE") },
                                placeholder = { Text("DELETE") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.error, focusedLabelColor = MaterialTheme.colorScheme.error, cursorColor = MaterialTheme.colorScheme.error),
                                isError = errorMessage.isNotEmpty()
                            )
                            if (errorMessage.isNotEmpty()) {
                                Text(errorMessage.ifEmpty { preferencesManager.getString("type_delete_exact") }, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                                    Text(preferencesManager.getString("back"), color = MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = { onConfirm(confirmTextInput) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    enabled = confirmTextInput.isNotEmpty()
                                ) {
                                    Text(preferencesManager.getString("delete_forever"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// (Giữ nguyên SettingItemCard, LanguageSelectionDialog, ThemeSelectionDialog ở dưới file như cũ)
@Composable
fun SettingItemCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, title, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Navigate", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun LanguageSelectionDialog(currentLanguage: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit, preferencesManager: PreferencesManager) {
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    val languages = listOf("English", "Tiếng Việt")
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(preferencesManager.getString("select_language"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(20.dp))
                languages.forEach { language ->
                    val isSelected = selectedLanguage == language
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        onClick = { selectedLanguage = language }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isSelected, onClick = { selectedLanguage = language }, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(language, style = MaterialTheme.typography.bodyLarge, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
                        Text(preferencesManager.getString("cancel"), color = MaterialTheme.colorScheme.primary)
                    }
                    Button(onClick = { onConfirm(selectedLanguage) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                        Text(preferencesManager.getString("save"))
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelectionDialog(isDarkTheme: Boolean, onDismiss: () -> Unit, onConfirm: (Boolean) -> Unit, preferencesManager: PreferencesManager) {
    var selectedTheme by remember { mutableStateOf(if (isDarkTheme) "Dark" else "Light") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(preferencesManager.getString("select_theme"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(20.dp))
                listOf("Light", "Dark").forEach { theme ->
                    val isSelected = selectedTheme == theme
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        onClick = { selectedTheme = theme }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isSelected, onClick = { selectedTheme = theme }, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(if (theme == "Light") preferencesManager.getString("light_mode") else preferencesManager.getString("dark_mode"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                Text(if (theme == "Light") preferencesManager.getString("light_mode_desc") else preferencesManager.getString("dark_mode_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
                        Text(preferencesManager.getString("cancel"), color = MaterialTheme.colorScheme.primary)
                    }
                    Button(onClick = { onConfirm(selectedTheme == "Dark") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                        Text(preferencesManager.getString("apply"))
                    }
                }
            }
        }
    }
}