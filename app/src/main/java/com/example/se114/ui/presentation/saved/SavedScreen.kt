package com.example.se114.ui.presentation.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.ui.presentation.components.PostFeed
import com.example.se114.ui.presentation.home.HomeTabs
import com.example.se114.ui.theme.AppTealDark

@Composable
fun SavedScreen(
    viewModel: SavedViewModel = hiltViewModel(),
    onNavigateToOtherProfile: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferencesManager = viewModel.preferencesManager
    val currentUserId = preferencesManager.userId

    // Tabs Titles
    val tabs = listOf(
        preferencesManager.getString("tab_support"),
        preferencesManager.getString("tab_service")
    )

    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts()
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppTealDark,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                Text(
                    text = preferencesManager.getString("saved_posts"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // [MỚI] Thêm Tabs y chang Home
        // Nếu HomeTabs báo lỗi đỏ, hãy copy đoạn code @Composable HomeTabs từ HomeScreen.kt dán xuống cuối file này
        HomeTabs(
            tabs = tabs,
            selectedTabIndex = uiState.selectedTabIndex,
            onTabSelected = viewModel::onTabSelected
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            // Hiển thị displayedPosts (đã lọc theo tab) thay vì savedPosts
            PostFeed(
                posts = uiState.displayedPosts,
                savedPostIds = uiState.allSavedPosts.map { it.id }.toSet(),
                preferencesManager = preferencesManager,
                currentUserId = currentUserId,
                onLikeClick = { postId -> viewModel.onToggleLike(postId) },
                onSaveClick = { postId -> viewModel.onUnsave(postId) },
                onHideClick = { /* Logic ẩn */ },
                onReportClick = { /* Logic báo cáo */ },
                onCommentClick = { /* Logic comment */ },
                onNavigateToOtherProfile = onNavigateToOtherProfile,
                onNavigateToProfile = onNavigateToProfile,
                emptyMessage = preferencesManager.getString("empty_saved_posts")
            )
        }
    }
}