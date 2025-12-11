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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.local.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDataScreen(
    preferencesManager: PreferencesManager,
    onBackClick: () -> Unit,
    viewModel: AccountDataViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Force recomposition khi đổi ngôn ngữ
    val currentLanguage = preferencesManager.languageState.value

    // Khởi tạo dữ liệu cho ViewModel từ Preferences (Chạy 1 lần)
    LaunchedEffect(Unit) {
        viewModel.setInitialData(
            address = preferencesManager.userAddress,
            phone = preferencesManager.userPhone,
            gender = preferencesManager.userGender,
            job = preferencesManager.userJob
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        preferencesManager.getString("account_data"),
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
            DataItemCard(
                icon = Icons.Default.LocationOn,
                title = preferencesManager.getString("address"),
                value = uiState.address,
                isEditable = true,
                onEditClick = viewModel::showAddressDialog
            )

            DataItemCard(
                icon = Icons.Default.Phone,
                title = preferencesManager.getString("phone_number"),
                value = uiState.phone,
                isEditable = false,
                subtitle = preferencesManager.getString("edit_in_settings")
            )

            DataItemCard(
                icon = Icons.Default.Person,
                title = preferencesManager.getString("gender"),
                value = uiState.gender,
                isEditable = true,
                onEditClick = viewModel::showGenderDialog
            )

            DataItemCard(
                icon = Icons.Default.Work,
                title = preferencesManager.getString("current_job"),
                value = uiState.currentJob,
                isEditable = true,
                onEditClick = viewModel::showJobDialog
            )
        }
    }

    // --- Dialogs & Logic Save (Logic Preferences nằm tại đây) ---

    if (uiState.isShowingAddressDialog) {
        EditTextDialog(
            title = preferencesManager.getString("edit_address"),
            currentValue = uiState.address,
            placeholder = preferencesManager.getString("enter_address"),
            onDismiss = viewModel::hideAddressDialog,
            onConfirm = { newValue ->
                // 1. Lưu vào Preferences (Logic cũ)
                preferencesManager.userAddress = newValue
                // 2. Cập nhật UI State
                viewModel.updateAddress(newValue)
            },
            preferencesManager = preferencesManager
        )
    }

    if (uiState.isShowingGenderDialog) {
        GenderSelectionDialog(
            currentGender = uiState.gender,
            onDismiss = viewModel::hideGenderDialog,
            onConfirm = { newGender ->
                preferencesManager.userGender = newGender
                viewModel.updateGender(newGender)
            },
            preferencesManager = preferencesManager
        )
    }

    if (uiState.isShowingJobDialog) {
        EditTextDialog(
            title = preferencesManager.getString("edit_job"),
            currentValue = uiState.currentJob,
            placeholder = preferencesManager.getString("enter_job"),
            onDismiss = viewModel::hideJobDialog,
            onConfirm = { newValue ->
                preferencesManager.userJob = newValue
                viewModel.updateJob(newValue)
            },
            preferencesManager = preferencesManager
        )
    }
}

// --- GIỮ NGUYÊN CÁC COMPOSABLE PHỤ TRỢ Ở DƯỚI ---

@Composable
fun DataItemCard(
    icon: ImageVector,
    title: String,
    value: String,
    isEditable: Boolean,
    subtitle: String? = null,
    onEditClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
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
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
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
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditTextDialog(
    title: String,
    currentValue: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    preferencesManager: PreferencesManager
) {
    var textValue by remember { mutableStateOf(currentValue) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(placeholder) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (title.contains(preferencesManager.getString("address"))) 3 else 1,
                    maxLines = if (title.contains(preferencesManager.getString("address"))) 4 else 1
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
                            if (textValue.isEmpty()) {
                                errorMessage = preferencesManager.getString("field_empty_error")
                            } else {
                                onConfirm(textValue)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(preferencesManager.getString("save"))
                    }
                }
            }
        }
    }
}

@Composable
fun GenderSelectionDialog(
    currentGender: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    preferencesManager: PreferencesManager
) {
    var selectedGender by remember { mutableStateOf(currentGender) }
    val genders = listOf(
        preferencesManager.getString("male"),
        preferencesManager.getString("female"),
        preferencesManager.getString("other"),
        preferencesManager.getString("prefer_not_to_say")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = preferencesManager.getString("select_gender"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                genders.forEach { gender ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedGender == gender)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (selectedGender == gender)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant
                        ),
                        onClick = { selectedGender = gender }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedGender == gender,
                                onClick = { selectedGender = gender },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = gender,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedGender == gender)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
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
                        onClick = { onConfirm(selectedGender) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(preferencesManager.getString("save"))
                    }
                }
            }
        }
    }
}