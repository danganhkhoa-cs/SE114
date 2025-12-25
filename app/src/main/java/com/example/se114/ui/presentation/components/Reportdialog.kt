package com.example.se114.ui.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.se114.local.PreferencesManager

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (reasonKey: String, description: String) -> Unit,
    preferencesManager: PreferencesManager,
    reasonKeys: List<String>,
    titleKey: String
) {
    var selectedReasonKey by remember { mutableStateOf("") }
    var showReasonDropdown by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // TIÊU ĐỀ
                Text(
                    text = preferencesManager.getString(titleKey),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = preferencesManager.getString("report_reason"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // DROPDOWN CHỌN LÝ DO
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showReasonDropdown = true
                                    showError = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedReasonKey.isEmpty()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selectedReasonKey.isEmpty()) 1.5.dp else 2.dp,
                                color = if (showError) MaterialTheme.colorScheme.error else if (selectedReasonKey.isEmpty()) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val displayText = if (selectedReasonKey.isEmpty()) {
                                    preferencesManager.getString("select_reason")
                                } else {
                                    preferencesManager.getString(selectedReasonKey)
                                }

                                Text(
                                    text = displayText,
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedReasonKey.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                                    color = if (selectedReasonKey.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = if (selectedReasonKey.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showReasonDropdown,
                            onDismissRequest = { showReasonDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.75f) // Dropdown width
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        ) {
                            reasonKeys.forEach { key ->
                                DropdownMenuItem(
                                    text = { Text(preferencesManager.getString(key), fontSize = 15.sp) },
                                    onClick = {
                                        selectedReasonKey = key
                                        showReasonDropdown = false
                                        showError = false
                                    }
                                )
                            }
                        }
                    }

                    if (showError) {
                        Text(
                            text = preferencesManager.getString("report_select_reason_required"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // MÔ TẢ
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(preferencesManager.getString("report_description"), fontSize = 14.sp) },
                    placeholder = { Text(preferencesManager.getString("report_description_hint"), fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    maxLines = 5
                )

                // NÚT BẤM
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(preferencesManager.getString("report_cancel"), fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (selectedReasonKey.isEmpty()) {
                                showError = true
                            } else {
                                // Gửi KEY (ví dụ: "report_reason_spam") lên server
                                onSubmit(selectedReasonKey, description)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(preferencesManager.getString("report_submit"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}