package com.example.se114.ui.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
// QUAN TRỌNG: Thêm 2 import này để sửa lỗi 'getValue' và 'setValue' khi dùng 'by'
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.se114.data.PostType
import com.example.se114.data.SelectionData
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.create_post.KeyDropdownMenu

@Composable
fun FilterDialog(
    currentCity: String,
    currentDistrict: String,
    currentCategory: String,
    currentTabPostType: PostType,
    preferencesManager: PreferencesManager,
    onDismiss: () -> Unit,
    onApply: (String, String, String) -> Unit
) {
    var selectedCity by remember { mutableStateOf(currentCity) }
    var selectedDistrict by remember { mutableStateOf(currentDistrict) }
    var selectedCategory by remember { mutableStateOf(currentCategory) }

    val districtOptions = remember(selectedCity) {
        if (selectedCity.isNotEmpty()) {
            SelectionData.locations[selectedCity] ?: emptyList()
        } else {
            emptyList()
        }
    }

    val categoryOptions = remember(currentTabPostType) {
        SelectionData.getCategoryKeys(currentTabPostType)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = preferencesManager.getString("filter_title"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // 1. Chọn Thành Phố
                KeyDropdownMenu(
                    options = SelectionData.locations.keys.toList(),
                    selectedKey = selectedCity,
                    onKeySelected = {
                        selectedCity = it
                        selectedDistrict = ""
                    },
                    label = preferencesManager.getString("label_city"),
                    preferencesManager = preferencesManager
                )

                // 2. Chọn Quận Huyện
                KeyDropdownMenu(
                    options = districtOptions,
                    selectedKey = selectedDistrict,
                    onKeySelected = { selectedDistrict = it },
                    label = preferencesManager.getString("label_district"),
                    enabled = selectedCity.isNotEmpty(),
                    preferencesManager = preferencesManager
                )

                // 3. Chọn Danh Mục
                KeyDropdownMenu(
                    options = categoryOptions,
                    selectedKey = selectedCategory,
                    onKeySelected = { selectedCategory = it },
                    label = preferencesManager.getString("label_category"),
                    preferencesManager = preferencesManager
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        selectedCity = ""
                        selectedDistrict = ""
                        selectedCategory = ""
                    }) {
                        Text(preferencesManager.getString("btn_clear_filter"), color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = {
                        onApply(selectedCity, selectedDistrict, selectedCategory)
                        onDismiss()
                    }) {
                        Text(preferencesManager.getString("btn_apply"))
                    }
                }
            }
        }
    }
}