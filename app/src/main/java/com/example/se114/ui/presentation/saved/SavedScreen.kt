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
import com.example.se114.ui.presentation.components.PostEventListener
import com.example.se114.ui.presentation.components.PostFeed
import com.example.se114.ui.presentation.home.HomeTabs // Import HomeTabs từ HomeScreen
import com.example.se114.ui.theme.AppTealDark

@Composable
fun SavedScreen(
    viewModel: SavedViewModel = hiltViewModel(),
    onNavigateToOtherProfile: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferencesManager = viewModel.preferencesManager
    val currentUserId = preferencesManager.userId

    val tabs = listOf(
        preferencesManager.getString("tab_support"),
        preferencesManager.getString("tab_service")
    )

    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts()
    }

    // WRAPPER: Tự động thêm tính năng Report, Comment, Hide, Snackbar cho SavedScreen
    PostEventListener(viewModel = viewModel) { onLike, onSave, onReport, onComment ->

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Header riêng của SavedScreen
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

            // Tái sử dụng Tabs từ Home
            HomeTabs(
                tabs = tabs,
                selectedTabIndex = uiState.selectedTabIndex,
                onTabSelected = viewModel::onTabSelected
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                PostFeed(
                    posts = uiState.displayedPosts,
                    savedPostIds = uiState.allSavedPosts.map { it.id }.toSet(),
                    preferencesManager = preferencesManager,
                    currentUserId = currentUserId,
                    // Mapping sự kiện
                    onLikeClick = { postId ->
                        val post = uiState.allSavedPosts.find { it.id == postId }
                        if (post != null) onLike(post)
                    },
                    onSaveClick = { postId ->
                        // Logic đặc biệt của SavedScreen:
                        // Khi click nút save ở đây nghĩa là muốn Un-save.
                        // Ta truyền 'true' (đang saved) vào onSave để ViewModel xử lý un-save.
                        val post = uiState.allSavedPosts.find { it.id == postId }
                        if (post != null) onSave(post, true)
                    },
                    onReportClick = { postId -> onReport(postId) },
                    onCommentClick = { post -> onComment(post) },
                    onNavigateToOtherProfile = onNavigateToOtherProfile,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToPostDetail = onNavigateToPostDetail,
                    emptyMessage = preferencesManager.getString("empty_saved_posts")
                )
            }
        }
    }
}