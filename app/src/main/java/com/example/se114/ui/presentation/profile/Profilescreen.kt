package com.example.se114.ui.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.se114.data.model.Review
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showReviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) {
            onLogout()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            scope.launch { snackbarHostState.showSnackbar(uiState.errorMessage ?: "Error") }
            viewModel.clearError()
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
                        .padding(top = 50.dp, bottom = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = preferencesManager.getString("profile_title"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val avatarUrl = preferencesManager.userAvatar

                            ProfileAvatarDisplay(
                                avatarModel = if (avatarUrl.isNotEmpty()) avatarUrl else null,
                                userName = uiState.userName,
                                size = 70.dp
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    uiState.userName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                // --- HIỂN THỊ RATING CỦA BẢN THÂN ---
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = String.format("%.1f", uiState.rating),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        // FIX: Đổi Color.Black thành onSurface để tự động chuyển trắng khi Dark Mode
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(${uiState.reviewCount})",
                                        style = MaterialTheme.typography.bodySmall,
                                        // FIX: Dùng onSurfaceVariant thay vì Gray cứng để rõ hơn trên nền tối
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = preferencesManager.getString("see_reviews"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        viewModel.loadMyReviews(reset = true)
                                        showReviewDialog = true
                                    }
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    uiState.userBio.ifBlank { "Chưa có tiểu sử" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }

                            IconButton(
                                onClick = viewModel::showEditProfileDialog,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    "Edit",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 2.dp
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

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showLogoutDialog() },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    "Logout",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                preferencesManager.getString("logout"),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                "Navigate",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isLoggingOut || uiState.isLoading) {
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

    if (uiState.isShowingEditProfileDialog) {
        EditProfileDialog(
            currentName = uiState.userName,
            currentBio = uiState.userBio,
            currentAvatarUrl = preferencesManager.userAvatar,
            onDismiss = viewModel::hideEditProfileDialog,
            onSave = { name, bio, uri, isDeleted ->
                // Gọi hàm saveProfileChanges khi nhấn Save
                viewModel.saveProfileChanges(name, bio, uri, isDeleted)
            },
            preferencesManager = preferencesManager
        )
    }

    if (uiState.isShowingLogoutDialog) {
        LogoutConfirmationDialog(
            onDismiss = viewModel::hideLogoutDialog,
            onConfirm = {
                viewModel.logout()
            },
            preferencesManager = preferencesManager
        )
    }

    if (showReviewDialog) {
        ReviewListDialogLocal(
            reviews = uiState.reviewsList,
            authorAvatars = uiState.reviewAuthorAvatars, // Truyền Map avatar mới
            totalCount = uiState.reviewCount,
            isLoading = uiState.isReviewsLoading,
            onDismiss = { showReviewDialog = false },
            onLoadMore = { viewModel.loadMyReviews(reset = false) },
            preferencesManager = preferencesManager
        )
    }
}

// Sửa lại hàm này để nhận Any? thay vì String để hỗ trợ cả Uri và String URL
@Composable
fun ProfileAvatarDisplay(
    avatarModel: Any?, // Có thể là String (Url), Uri, hoặc null
    userName: String,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(2.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (avatarModel != null && avatarModel.toString().isNotEmpty()) {
            AsyncImage(
                model = avatarModel,
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.4).sp
                )
            }
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    title,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentBio: String,
    currentAvatarUrl: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Uri?, Boolean) -> Unit, // Callback trả về tất cả thay đổi
    preferencesManager: PreferencesManager
) {
    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }
    var errorMessage by remember { mutableStateOf("") }

    // State tạm thời cho Avatar
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var isAvatarDeleted by remember { mutableStateOf(false) }

    // Logic hiển thị Avatar Preview trong Dialog
    // Ưu tiên: Ảnh mới chọn > Trạng thái xóa > Ảnh hiện tại
    val previewModel: Any? = when {
        selectedAvatarUri != null -> selectedAvatarUri
        isAvatarDeleted -> null
        currentAvatarUrl.isNotEmpty() -> currentAvatarUrl
        else -> null
    }

    // Kiểm tra xem nút Xóa có nên hiển thị không (Có ảnh để xóa không?)
    // Hiện nút xóa nếu: (Có ảnh mới) HOẶC (Chưa xóa VÀ Có ảnh cũ)
    val showDeleteButton = selectedAvatarUri != null || (!isAvatarDeleted && currentAvatarUrl.isNotEmpty())

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Chỉ cập nhật state tạm, chưa upload
                selectedAvatarUri = uri
                isAvatarDeleted = false
            }
        }
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Avatar Clickable Area
                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Box(modifier = Modifier.clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        ProfileAvatarDisplay(
                            avatarModel = previewModel,
                            userName = name,
                            size = 80.dp
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.inverseSurface, CircleShape)
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Change Avatar",
                                tint = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Nút Xóa Avatar (Chỉ cập nhật state tạm)
                    if (showDeleteButton) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                                .clickable {
                                    // Đánh dấu là đã xóa, bỏ chọn ảnh mới
                                    isAvatarDeleted = true
                                    selectedAvatarUri = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        preferencesManager.getString("change_avatar"),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    preferencesManager.getString("edit_profile"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = "" },
                    label = { Text(preferencesManager.getString("name")) },
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(preferencesManager.getString("bio")) },
                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(preferencesManager.getString("cancel"), color = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = {
                            if (name.isEmpty()) errorMessage = preferencesManager.getString("name_empty_error")
                            else onSave(name, bio, selectedAvatarUri, isAvatarDeleted)
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

// Định nghĩa lại Dialog tại đây để dùng cho ProfileScreen (Code duplicate để dễ copy-paste 1 file)
@Composable
fun ReviewListDialogLocal(
    reviews: List<Review>,
    authorAvatars: Map<String, String>, // Tham số mới
    totalCount: Int,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit,
    preferencesManager: PreferencesManager
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${preferencesManager.getString("rating_reviews")} ($totalCount)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (reviews.isEmpty() && !isLoading) {
                    Text(
                        text = preferencesManager.getString("no_reviews_yet"),
                        modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally),
                        color = Color.Gray
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(reviews.size) { index ->
                            ReviewItemLocal(
                                review = reviews[index],
                                authorAvatars = authorAvatars // Truyền Map xuống
                            )
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(top = 8.dp))
                        }

                        item {
                            if (isLoading) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            } else if (reviews.size < totalCount) {
                                TextButton(
                                    onClick = onLoadMore,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(preferencesManager.getString("load_more_reviews"))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(preferencesManager.getString("close"))
                }
            }
        }
    }
}

@Composable
fun ReviewItemLocal(
    review: Review,
    authorAvatars: Map<String, String> // Tham số mới
) {
    // Ưu tiên lấy avatar từ Map (tươi mới), nếu không có mới dùng cái trong Review (cũ)
    val avatarToShow = authorAvatars[review.reviewerId] ?: review.reviewerAvatar

    Row(modifier = Modifier.fillMaxWidth()) {
        if (avatarToShow.isNotEmpty()) {
            // Dùng AsyncImage để load ảnh, hỗ trợ cache
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarToShow)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(40.dp).background(Color.LightGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(review.reviewerName.take(1).uppercase(), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(review.reviewerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { i ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (i < review.rating) Color(0xFFFFC107) else Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (review.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(review.comment, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}