package com.example.se114.ui.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.data.Post
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.components.CommentBottomSheet
import com.example.se114.ui.presentation.components.PostFeed
import com.example.se114.ui.presentation.components.ReportDialog
import com.example.se114.ui.theme.AppTealDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    preferencesManager: PreferencesManager,
    onNavigateToNotification: () -> Unit = {},
    onNavigateToOtherProfile: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessageText by remember { mutableStateOf("") }
    var showCommentSheet by remember { mutableStateOf(false) }
    var reportPostId by remember { mutableStateOf<String?>(null) }
    var selectedPostForComment by remember { mutableStateOf<Post?>(null) }

    val currentUserId = preferencesManager.userId

    // Logic Snackbar
    LaunchedEffect(uiState.currentMessage) {
        if (uiState.currentMessage != HomeMessage.NONE) {
            val message = when(uiState.currentMessage) {
                HomeMessage.SAVED -> preferencesManager.getString("post_saved")
                HomeMessage.UNSAVED -> preferencesManager.getString("unsave_post")
                HomeMessage.HIDDEN -> preferencesManager.getString("post_hidden")
                HomeMessage.REPORT_SUCCESS -> preferencesManager.getString("report_success")
                HomeMessage.REPORT_DUPLICATE -> preferencesManager.getString("report_duplicate_post") // Nên đưa vào StringResources
                HomeMessage.REPORT_ERROR -> preferencesManager.getString("unknown_error")
                else -> ""
            }
            if (message.isNotEmpty()) {
                snackbarMessageText = message
                showSnackbar = true
            }
            viewModel.onMessageShown()
        }
    }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            kotlinx.coroutines.delay(2000)
            showSnackbar = false
        }
    }

    val tabs = listOf(
        preferencesManager.getString("tab_support"),
        preferencesManager.getString("tab_service")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            HomeHeader(
                notificationCount = uiState.notificationUnreadCount,
                onNavigateToNotification = onNavigateToNotification
            )

            HomeTabs(
                tabs = tabs,
                selectedTabIndex = uiState.selectedTabIndex,
                onTabSelected = viewModel::onTabSelected
            )

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.onRefresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                // SỬ DỤNG COMPONENT CHUNG
                PostFeed(
                    posts = uiState.displayedPosts,
                    savedPostIds = uiState.savedPostIds,
                    preferencesManager = preferencesManager,
                    onLikeClick = { postId -> viewModel.onToggleLike(postId) },
                    onSaveClick = { postId -> viewModel.onToggleSave(postId) },
                    onHideClick = { postId -> viewModel.onHidePost(postId) },
                    onReportClick = { clickedPostId -> reportPostId = clickedPostId },
                    onCommentClick = { post ->
                        selectedPostForComment = post
                        showCommentSheet = true
                    },
                    onNavigateToOtherProfile = onNavigateToOtherProfile,
                    onNavigateToProfile = onNavigateToProfile,
                    currentUserId = currentUserId
                )
            }
        }

        if (showSnackbar) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(snackbarMessageText, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (showCommentSheet && selectedPostForComment != null) {
            CommentBottomSheet(
                onDismiss = {
                    showCommentSheet = false
                    selectedPostForComment = null
                },
                postId = selectedPostForComment!!.id,
                preferencesManager = preferencesManager
            )
        }

        if (reportPostId != null) {
            ReportDialog(
                onDismiss = { reportPostId = null }, // Reset về null để đóng dialog
                onSubmit = { reason, description ->
                    // Gọi ViewModel với ID đã lưu
                    viewModel.onSubmitReport(reportPostId!!, reason, description)
                },
                preferencesManager = preferencesManager
            )
        }
    }
}

// --- SUB COMPONENTS ---

@Composable
fun HomeHeader(
    notificationCount: Int,
    onNavigateToNotification: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppTealDark,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(90.dp)
        ) {
            val blobAlpha = 0.1f
            Canvas(modifier = Modifier.size(100.dp).align(Alignment.TopEnd).offset(x = 30.dp, y = (-20).dp)) {
                drawCircle(color = Color.White.copy(alpha = blobAlpha), radius = size.minDimension / 2)
            }
            Canvas(modifier = Modifier.size(60.dp).align(Alignment.TopStart).offset(x = (-15).dp, y = 10.dp)) {
                drawCircle(color = Color.White.copy(alpha = blobAlpha), radius = size.minDimension / 2)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Filled.LocationOn, "Logo", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                    Text("LocaSOS", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.2.sp, style = MaterialTheme.typography.headlineMedium)
                }

                Box {
                    IconButton(
                        onClick = onNavigateToNotification,
                        modifier = Modifier.size(48.dp).background(color = Color.White.copy(alpha = 0.15f), shape = CircleShape)
                    ) {
                        Icon(Icons.Outlined.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    if (notificationCount > 0) {
                        Badge(
                            containerColor = Color(0xFFFF1744),
                            contentColor = Color.White,
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp)
                        ) {
                            Text(if (notificationCount > 9) "9+" else notificationCount.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeTabs(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background, shadowElevation = 0.dp) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).background(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(28.dp)).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    val selectedBg = MaterialTheme.colorScheme.primary
                    val selectedContentColor = MaterialTheme.colorScheme.onPrimary
                    val unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f).fillMaxHeight()
                            .then(if (isSelected) Modifier.shadow(4.dp, RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)).background(selectedBg, RoundedCornerShape(24.dp)).border(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)) else Modifier.background(Color.Transparent).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(24.dp)))
                            .clickable { onTabSelected(index) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(if (index == 0) Icons.Filled.Public else Icons.Filled.People, null, tint = if (isSelected) selectedContentColor else unselectedContentColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 15.sp, color = if (isSelected) selectedContentColor else unselectedContentColor)
                        }
                    }
                }
            }
        }
    }
}