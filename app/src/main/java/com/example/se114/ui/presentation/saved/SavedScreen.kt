package com.example.se114.ui.presentation.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.home.PostCard // Hoặc .components.PostCard tùy vị trí bạn đặt file
import com.example.se114.ui.theme.AppTealDark

@Composable
fun SavedScreen(
    viewModel: SavedViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Khởi tạo PreferencesManager để quản lý ngôn ngữ
    // remember giúp giữ lại instance này qua các lần recomposition
    val preferencesManager = remember { PreferencesManager(context) }

    // Reload dữ liệu mỗi khi màn hình hiển thị lại
    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- HEADER ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppTealDark,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp)
            ) {
                // [THAY ĐỔI 1] Áp dụng đa ngôn ngữ cho tiêu đề
                // Key "Saved Posts" đã có trong file StringResources.kt của bạn
                Text(
                    text = preferencesManager.getString("Saved Posts"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // --- CONTENT ---
        if (uiState.savedPosts.isEmpty()) {
            // Màn hình trống
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BookmarkRemove,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // [THAY ĐỔI 2] Áp dụng đa ngôn ngữ cho thông báo trống
                    // Key "empty_saved_posts" đã có trong file StringResources.kt
                    Text(
                        text = preferencesManager.getString("empty_saved_posts"),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // Danh sách bài đã lưu
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.savedPosts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        isSaved = true, // Luôn true ở màn hình Saved
                        preferencesManager = preferencesManager, // Truyền xuống để PostCard tự dịch menu
                        onLikeClick = { /* Xử lý like nếu cần */ },
                        onSaveClick = {
                            // Xử lý bỏ lưu ngay tại màn hình Saved
                            viewModel.onUnsave(post.id)
                        },
                        onHideClick = { /* Xử lý ẩn */ },
                        onReportSubmitted = { /* Xử lý báo cáo */ },
                        onCommentClick = { /* Mở comment */ },
                        onAvatarClick = { /* Chuyển đến profile */ }
                    )
                }
            }
        }
    }
}