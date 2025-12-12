package com.example.se114.ui.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.data.Post
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.components.CommentBottomSheet
import com.example.se114.ui.presentation.components.ReportDialog
import com.example.se114.ui.theme.AppTealDark

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToNotification: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    // Giữ nguyên PreferencesManager ở Screen để lấy chuỗi
    val preferencesManager = remember { PreferencesManager(context) }

    // Quản lý UI cục bộ (bottom sheet, snackbar)
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessageText by remember { mutableStateOf("") }

    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedPostForComment by remember { mutableStateOf<Post?>(null) }

    // Xử lý Side Effect: Hiển thị Snackbar dựa trên thay đổi từ ViewModel
    LaunchedEffect(uiState.currentMessage) {
        if (uiState.currentMessage != HomeMessage.NONE) {
            val message = when(uiState.currentMessage) {
                HomeMessage.SAVED -> preferencesManager.getString("post_saved")
                HomeMessage.UNSAVED -> preferencesManager.getString("unsave_post") // Tùy chọn
                HomeMessage.HIDDEN -> preferencesManager.getString("post_hidden")
                HomeMessage.REPORT_SUCCESS -> preferencesManager.getString("report_success")
                else -> ""
            }

            if (message.isNotEmpty()) {
                snackbarMessageText = message
                showSnackbar = true
            }
            viewModel.onMessageShown()
        }
    }

    // Tắt Snackbar sau 2s
    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            kotlinx.coroutines.delay(2000)
            showSnackbar = false
        }
    }

    val tabs = listOf(
        preferencesManager.getString("tab_everyone"),
        preferencesManager.getString("tab_foryou")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- HEADER ---
            HomeHeader(
                notificationCount = uiState.notificationUnreadCount,
                onNavigateToNotification = onNavigateToNotification
            )

            // --- TABS ---
            HomeTabs(
                tabs = tabs,
                selectedTabIndex = uiState.selectedTabIndex,
                onTabSelected = viewModel::onTabSelected
            )

            // --- SAVED FILTER BUTTON ---
            HomeSavedFilter(
                isVisible = uiState.selectedTabIndex == 1,
                isShowingSavedPosts = uiState.isShowingSavedPosts,
                label = preferencesManager.getString("tab_saved"),
                onClick = viewModel::toggleSavedFilter
            )

            // --- LIST ---
            val displayedPosts = uiState.displayedPosts

            if (displayedPosts.isEmpty() && uiState.isShowingSavedPosts && uiState.selectedTabIndex == 1) {
                EmptySavedState(message = preferencesManager.getString("empty_saved_posts"))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = displayedPosts,
                        key = { it.id }
                    ) { post ->
                        PostCard(
                            post = post,
                            isSaved = post.id in uiState.savedPostIds,
                            preferencesManager = preferencesManager, // Truyền xuống để lấy chuỗi trong Menu
                            onLikeClick = { viewModel.onToggleLike(post.id) },
                            onSaveClick = { viewModel.onToggleSave(post.id) },
                            onHideClick = { viewModel.onHidePost(post.id) },
                            onReportSubmitted = { viewModel.onReportSubmitted() },
                            onCommentClick = {
                                selectedPostForComment = post
                                showCommentSheet = true
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // --- SNACKBAR ---
        if (showSnackbar) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Snackbar(
                    modifier = Modifier
                        .padding(bottom = 120.dp)
                        .padding(horizontal = 16.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(snackbarMessageText, fontWeight = FontWeight.Medium)
                }
            }
        }

        // --- COMMENT SHEET ---
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
    }
}

// --- SUB COMPONENTS (Tách nhỏ để dễ đọc) ---

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
            // (Giữ nguyên Canvas blobs của bạn)
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

@Composable
fun HomeSavedFilter(
    isVisible: Boolean,
    isShowingSavedPosts: Boolean,
    label: String,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
            Surface(
                modifier = Modifier
                    .height(40.dp)
                    .shadow(if (isShowingSavedPosts) 8.dp else 2.dp, RoundedCornerShape(20.dp), spotColor = if (isShowingSavedPosts) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f))
                    .clickable { onClick() },
                shape = RoundedCornerShape(20.dp),
                color = if (isShowingSavedPosts) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (isShowingSavedPosts) 1.5.dp else 1.dp, if (isShowingSavedPosts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(if (isShowingSavedPosts) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null, tint = if (isShowingSavedPosts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label, fontSize = 14.sp, fontWeight = if (isShowingSavedPosts) FontWeight.Bold else FontWeight.Medium, color = if (isShowingSavedPosts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun EmptySavedState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(bottom = 100.dp)) {
            Icon(Icons.Default.BookmarkBorder, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    isSaved: Boolean,
    preferencesManager: PreferencesManager,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onHideClick: () -> Unit,
    onReportSubmitted: () -> Unit,
    onCommentClick: () -> Unit
) {
    // PostCard giờ là Stateless, chỉ hiển thị dữ liệu từ post
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().background(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), Color.Transparent))).padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(50.dp).shadow(6.dp, CircleShape), shape = CircleShape, color = MaterialTheme.colorScheme.primary, border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(post.userName.first().toString(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(post.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(post.timeAgo, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Box {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(36.dp)) {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.width(220.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                        Text(if (isSaved) preferencesManager.getString("unsave_post") else preferencesManager.getString("save_post"), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = { showMenu = false; onSaveClick() },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                                        Text(preferencesManager.getString("hide_post"), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = { showMenu = false; onHideClick() },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                                        Text(preferencesManager.getString("report_post"), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                onClick = { showMenu = false; showReportDialog = true },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                                        Column {
                                            Text("${preferencesManager.getString("about_user")} ${post.userName}", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                },
                                onClick = { showMenu = false },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                if (showReportDialog) {
                    ReportDialog(
                        onDismiss = { showReportDialog = false },
                        onSubmit = { _, _ -> onReportSubmitted() },
                        preferencesManager = preferencesManager
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(post.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 21.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, "Location", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(post.location, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(brush = Brush.horizontalGradient(colors = listOf(Color.Transparent, MaterialTheme.colorScheme.outlineVariant, Color.Transparent))))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val likeBg = if (post.isLiked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                val likeContentColor = if (post.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(shape = RoundedCornerShape(14.dp), color = likeBg, modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.clickable { onLikeClick() }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (post.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Like", tint = likeContentColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(post.likeCount.toString(), color = likeContentColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.clickable { onCommentClick() }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, "Comment", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(post.commentCount.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(48.dp)) {
                    IconButton(onClick = { /* Handle share */ }) {
                        Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}