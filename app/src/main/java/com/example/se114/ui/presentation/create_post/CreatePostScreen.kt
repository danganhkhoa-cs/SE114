package com.example.se114.ui.presentation.create_post

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.data.PostType
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.AppTealLight
import com.example.se114.ui.theme.AppTealNeon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel = hiltViewModel(),
    preferencesManager: PreferencesManager,
    onPostSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isDark = preferencesManager.isDarkMode
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val postButtonColor = if (isDark) AppTealNeon else AppTealDark
    val postButtonContentColor = if (isDark) Color(0xFF00363D) else Color.White

    val errorMessage = preferencesManager.getString("fill_all_fields")

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            Toast.makeText(context, preferencesManager.getString("post_success"), Toast.LENGTH_SHORT).show()
            viewModel.resetState()
            onPostSuccess()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        preferencesManager.getString("create_post_title"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = textColor
                ),
                actions = {
                    Button(
                        onClick = {
                            if (uiState.content.isBlank() || uiState.selectedCity.isEmpty() || uiState.selectedDistrict.isEmpty() || uiState.selectedCategory.isEmpty()) {
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.createPost()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = postButtonColor,
                            contentColor = postButtonContentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.padding(end = 8.dp).height(36.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = postButtonContentColor, strokeWidth = 2.dp)
                        } else {
                            Text(preferencesManager.getString("post_button"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 1. User Info & Avatar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = AppTealLight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (preferencesManager.userName.isNotEmpty()) preferencesManager.userName.first().toString() else "?",
                            fontWeight = FontWeight.Bold,
                            color = AppTealDark,
                            fontSize = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = preferencesManager.userName,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. POST TYPE SELECTOR (Support vs Service)
            val selectedColor = if(isDark) AppTealNeon else AppTealDark
            val unselectedColor = Color.Transparent
            val selectedTextColor = if(isDark) Color(0xFF00363D) else Color.White
            val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(4.dp),
            ) {
                // Tab Support
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (uiState.selectedPostType == PostType.SUPPORT) selectedColor else unselectedColor
                        )
                        .clickable { viewModel.onPostTypeChanged(PostType.SUPPORT) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preferencesManager.getString("tab_support"),
                        fontWeight = if (uiState.selectedPostType == PostType.SUPPORT) FontWeight.Bold else FontWeight.Medium,
                        color = if (uiState.selectedPostType == PostType.SUPPORT) selectedTextColor else unselectedTextColor
                    )
                }

                // Tab Service
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (uiState.selectedPostType == PostType.SERVICE) selectedColor else unselectedColor
                        )
                        .clickable { viewModel.onPostTypeChanged(PostType.SERVICE) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preferencesManager.getString("tab_service"),
                        fontWeight = if (uiState.selectedPostType == PostType.SERVICE) FontWeight.Bold else FontWeight.Medium,
                        color = if (uiState.selectedPostType == PostType.SERVICE) selectedTextColor else unselectedTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. DROPDOWNS (City, District, Category)

            // City Dropdown
            SimpleDropdown(
                label = preferencesManager.getString("label_city"),
                placeholder = preferencesManager.getString("select_city"),
                options = viewModel.cities,
                selectedOption = uiState.selectedCity,
                onOptionSelected = viewModel::onCitySelected,
                isDark = isDark,
                preferenceManager = preferencesManager
            )

            Spacer(modifier = Modifier.height(12.dp))

            val availableDistricts = viewModel.getDistricts(uiState.selectedCity)
            SimpleDropdown(
                label = preferencesManager.getString("label_district"),
                placeholder = if(uiState.selectedCity.isEmpty()) preferencesManager.getString("select_city") else preferencesManager.getString("select_district"),
                options = availableDistricts,
                selectedOption = uiState.selectedDistrict,
                onOptionSelected = viewModel::onDistrictSelected,
                enabled = uiState.selectedCity.isNotEmpty(),
                isDark = isDark,
                preferenceManager = preferencesManager
            )

            Spacer(modifier = Modifier.height(12.dp))

            val availableCategories = viewModel.getCategories(uiState.selectedPostType)
            SimpleDropdown(
                label = preferencesManager.getString("label_category"),
                placeholder = preferencesManager.getString("select_category"),
                options = availableCategories,
                selectedOption = uiState.selectedCategory,
                onOptionSelected = viewModel::onCategorySelected,
                isDark = isDark,
                preferenceManager = preferencesManager
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Input Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.content.isEmpty()) {
                    Text(
                        text = preferencesManager.getString("what_on_your_mind"),
                        style = TextStyle(fontSize = 18.sp, color = hintColor),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
                BasicTextField(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChanged,
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        color = textColor,
                        lineHeight = 26.sp
                    ),
                    cursorBrush = SolidColor(postButtonColor),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// --- Helper Component for Dropdown ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDropdown(
    label: String,
    placeholder: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    enabled: Boolean = true,
    isDark: Boolean,
    preferenceManager: PreferencesManager
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if(enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = preferenceManager.getString(selectedOption),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if(isDark) AppTealNeon else AppTealDark,
                focusedLabelColor = if(isDark) AppTealNeon else AppTealDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            ),
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            enabled = enabled
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(preferenceManager.getString(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}