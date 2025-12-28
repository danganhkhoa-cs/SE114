package com.example.se114.ui.presentation.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.data.PostType
import com.example.se114.ui.presentation.components.FilterDialog
import com.example.se114.ui.presentation.components.PostEventListener
import com.example.se114.ui.presentation.components.PostFeed
import com.example.se114.ui.presentation.home.HomeTabs
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

    // State cho Filter Dialog
    var showFilterDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        preferencesManager.getString("tab_support"),
        preferencesManager.getString("tab_service")
    )

    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts()
    }

    // Hiển thị Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            currentCity = uiState.filterCity,
            currentDistrict = uiState.filterDistrict,
            currentCategory = uiState.filterCategory,
            currentTabPostType = if (uiState.selectedTabIndex == 0) PostType.SUPPORT else PostType.SERVICE,
            preferencesManager = preferencesManager,
            onDismiss = { showFilterDialog = false },
            onApply = { city, district, category ->
                viewModel.applyFilter(city, district, category)
            }
        )
    }

    PostEventListener(viewModel = viewModel) { onLike, onSave, onReport, onComment ->

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppTealDark,
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
                    // Tiêu đề ở giữa
                    Text(
                        text = preferencesManager.getString("saved_posts"),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Nút Filter ở góc phải
                    IconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = "Filter",
                            tint = Color.White
                        )
                    }

                    // Dấu chấm đỏ nếu đang filter
                    if (uiState.filterCity.isNotEmpty() || uiState.filterCategory.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = (-2).dp)
                                .size(10.dp)
                                .background(Color(0xFFFF1744), CircleShape)
                        )
                    }
                }
            }

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
                    onLikeClick = { postId ->
                        val post = uiState.allSavedPosts.find { it.id == postId }
                        if (post != null) onLike(post)
                    },
                    onSaveClick = { postId ->
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