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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onLogout: () -> Unit,
    viewModel: SettingsScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentLanguage = preferencesManager.languageState.value
    val context = LocalContext.current

    // Hiển thị thông báo lỗi nếu có
    LaunchedEffect(uiState.deleteError) {
        if (uiState.deleteError.isNotEmpty()) {
            Toast.makeText(context, uiState.deleteError, Toast.LENGTH_SHORT).show()
        }
    }

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
            // Language Setting
            SettingItem(
                icon = Icons.Default.Language,
                title = preferencesManager.getString("language"),
                value = currentLanguage,
                onClick = viewModel::showLanguageDialog
            )

            // Theme Setting
            SettingItem(
                icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                title = preferencesManager.getString("dark_mode"),
                value = if (isDarkTheme) preferencesManager.getString("on") else preferencesManager.getString("off"),
                onClick = viewModel::showThemeDialog
            )

            // Block List
            SettingItem(
                icon = Icons.Default.Block,
                title = preferencesManager.getString("block_list"),
                value = "${uiState.blockedUsers.size} ${preferencesManager.getString("users")}",
                onClick = viewModel::showBlockListDialog
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Delete Account Button
            Button(
                onClick = viewModel::showDeleteAccountDialog,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        preferencesManager.getString("delete_account"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // Loading Overlay
    if (uiState.isDeleting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }

    // --- Dialogs ---

    if (uiState.isShowingLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onDismiss = viewModel::hideLanguageDialog,
            onConfirm = { selectedLang ->
                viewModel.updateLanguage(selectedLang)
                preferencesManager.language = selectedLang
            },
            preferencesManager = preferencesManager
        )
    }

    if (uiState.isShowingThemeDialog) {
        ThemeSelectionDialog(
            isDarkTheme = isDarkTheme,
            onDismiss = viewModel::hideThemeDialog,
            onConfirm = { isDark ->
                viewModel.updateTheme(isDark)
                preferencesManager.isDarkMode = isDark
                onThemeChange(isDark)
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
            error = uiState.deleteError,
            onDismiss = viewModel::hideDeleteAccountDialog,
            onNext = { password ->
                // Sửa lại theo đúng giao diện cũ: truyền cả step và password
                viewModel.onDeleteNextStep(uiState.deleteStep, password)
            },
            onPrevious = viewModel::onDeletePreviousStep,
            onConfirmDelete = { confirmText ->
                // Logic cũ: truyền callback onLogout vào đây
                viewModel.confirmDeleteAccount(confirmText) {
                    onLogout()
                }
            },
            preferencesManager = preferencesManager
        )
    }
}

// --- SUB COMPOSABLES (Giữ nguyên như file bạn gửi) ---

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
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
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    preferencesManager: PreferencesManager
) {
    val languages = listOf("English", "Tiếng Việt")
    var selected by remember { mutableStateOf(currentLanguage) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = preferencesManager.getString("language"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(20.dp))
                languages.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = language }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selected == language),
                            onClick = { selected = language }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = language,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(preferencesManager.getString("cancel"))
                    }
                    Button(onClick = { onConfirm(selected) }, modifier = Modifier.weight(1f)) {
                        Text(preferencesManager.getString("apply"))
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
    preferencesManager: PreferencesManager
) {
    var selectedIsDark by remember { mutableStateOf(isDarkTheme) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = preferencesManager.getString("dark_mode"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedIsDark = false }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !selectedIsDark,
                        onClick = { selectedIsDark = false }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = preferencesManager.getString("light_mode"), color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedIsDark = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedIsDark,
                        onClick = { selectedIsDark = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = preferencesManager.getString("dark_mode"), color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(preferencesManager.getString("cancel"))
                    }
                    Button(onClick = { onConfirm(selectedIsDark) }, modifier = Modifier.weight(1f)) {
                        Text(preferencesManager.getString("apply"))
                    }
                }
            }
        }
    }
}

@Composable
fun BlockListDialog(
    blockedUsers: List<String>,
    onDismiss: () -> Unit,
    onUnblock: (String) -> Unit,
    preferencesManager: PreferencesManager
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = preferencesManager.getString("block_list"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (blockedUsers.isEmpty()) {
                    Text(
                        text = preferencesManager.getString("no_blocked_users"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        blockedUsers.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(user, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                Button(
                                    onClick = { onUnblock(user) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(preferencesManager.getString("unblock"), fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(preferencesManager.getString("close"))
                }
            }
        }
    }
}

@Composable
fun DeleteAccountDialog(
    step: Int,
    error: String,
    onDismiss: () -> Unit,
    onNext: (String) -> Unit,
    onPrevious: () -> Unit,
    onConfirmDelete: (String) -> Unit,
    preferencesManager: PreferencesManager
) {
    var passwordInput by remember { mutableStateOf("") }
    var confirmTextInput by remember { mutableStateOf("") }

    LaunchedEffect(step) {
        if (step == 2) passwordInput = ""
        if (step == 3) confirmTextInput = ""
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = preferencesManager.getString("delete_account_title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                when (step) {
                    1 -> Text(
                        text = preferencesManager.getString("delete_warning_desc"),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    2 -> {
                        Text(
                            text = preferencesManager.getString("enter_password_continue"),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text(preferencesManager.getString("password")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    3 -> {
                        Text(
                            text = preferencesManager.getString("type_delete"),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmTextInput,
                            onValueChange = { confirmTextInput = it },
                            label = { Text("DELETE") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.error,
                                unfocusedBorderColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (error.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (step == 1) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text(preferencesManager.getString("cancel"))
                        }
                        Button(
                            onClick = { onNext("") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(preferencesManager.getString("next"))
                        }
                    } else {
                        OutlinedButton(onClick = onPrevious, modifier = Modifier.weight(1f)) {
                            Text(preferencesManager.getString("back"))
                        }
                        Button(
                            onClick = {
                                if (step == 2) onNext(passwordInput)
                                else onConfirmDelete(confirmTextInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (step == 3) preferencesManager.getString("delete_forever") else preferencesManager.getString("next"))
                        }
                    }
                }
            }
        }
    }
}