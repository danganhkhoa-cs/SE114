package com.example.se114.ui.presentation.create_post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.se114.local.PreferencesManager

@Composable
fun KeyDropdownMenu(
    options: List<String>, // Danh sách các Key (vd: "city_hcm", "cat_repair")
    selectedKey: String,   // Key đang được chọn
    onKeySelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDark: Boolean = false,
    preferencesManager: PreferencesManager
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // Chuyển đổi Key sang Text hiển thị (Dùng StringResources thông qua PreferencesManager)
    val displayText = if (selectedKey.isNotEmpty()) {
        preferencesManager.getString(selectedKey)
    } else {
        ""
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label, fontSize = MaterialTheme.typography.bodySmall.fontSize) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, "content description")
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isDark) Color.Gray else Color.LightGray,
                disabledBorderColor = Color.LightGray.copy(alpha = 0.5f),
                disabledTextColor = Color.Gray.copy(alpha = 0.5f),
                disabledLabelColor = Color.Gray.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    textFieldSize = coordinates.size.toSize()
                }
                .clickable(enabled = enabled) { expanded = !expanded }
        )
        // Lớp phủ trong suốt để bắt sự kiện click thay cho TextField
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { expanded = !expanded }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                .background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { key ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = preferencesManager.getString(key),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onKeySelected(key)
                        expanded = false
                    }
                )
            }
        }
    }
}