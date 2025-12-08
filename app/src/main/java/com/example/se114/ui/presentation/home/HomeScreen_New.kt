package com.example.se114.ui.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.components.CommentBottomSheet
import com.example.se114.ui.presentation.components.ReportDialog
import com.example.se114.ui.theme.AppTealDark

data class Post(
    val id: Int,
    val userName: String,
    val userAvatar: String,
    val timeAgo: String,
    val content: String,
    val location: String,
    val imageUrl: String? = null,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean = false
)

@Composable
fun HomeScreen(
    onNavigateToNotification: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    var selectedTabIndex by remember { mutableStateOf(0) }

    // State để quản lý việc hiển thị bài đã lưu trong tab "For You"
    var isShowingSavedPosts by remember { mutableStateOf(false) }

    // Reset filter khi chuyển tab
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 0) {
            isShowingSavedPosts = false
        }
    }

    var notificationUnreadCount by remember { mutableStateOf(5) }

    // Quay lại 2 tabs cơ bản
    val tabs = listOf(
        preferencesManager.getString("tab_everyone"),
        preferencesManager.getString("tab_foryou")
    )

    val hiddenPostIds = remember { mutableStateListOf<Int>() }
    // Giả sử có sẵn 1 bài đã lưu (id 2) để demo
    val savedPostIds = remember { mutableStateListOf<Int>() }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedPostForComment by remember { mutableStateOf<Post?>(null) }

    val samplePosts = remember {
        listOf(
            Post(
                id = 1,
                userName = "Nguyen Van A",
                userAvatar = "",
                timeAgo = "2 hours ago",
                content = "Need urgent help! Flooding in my area. Anyone nearby can assist?",
                location = "District 1, HCMC",
                likeCount = 24,
                commentCount = 8
            ),
            Post(
                id = 2,
                userName = "Tran Thi B",
                userAvatar = "",
                timeAgo = "5 hours ago",
                content = "Medical emergency. Looking for nearby hospital or ambulance.",
                location = "District 3, HCMC",
                likeCount = 45,
                commentCount = 12
            ),
            Post(
                id = 3,
                userName = "Le Van C",
                userAvatar = "",
                timeAgo = "1 day ago",
                content = "Lost pet - Golden Retriever. Last seen near Landmark 81. Please help!",
                location = "Binh Thanh, HCMC",
                likeCount = 67,
                commentCount = 23
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- HEADER / APP BAR ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppTealDark,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val blobAlpha = 0.1f

                    Canvas(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 30.dp, y = (-20).dp)
                    ) {
                        drawCircle(
                            color = Color.White.copy(alpha = blobAlpha),
                            radius = size.minDimension / 2
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.TopStart)
                            .offset(x = (-15).dp, y = 10.dp)
                    ) {
                        drawCircle(
                            color = Color.White.copy(alpha = blobAlpha),
                            radius = size.minDimension / 2
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .align(Alignment.Center),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = "Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Text(
                                text = "LocaSOS",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.2.sp,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }

                        Box {
                            IconButton(
                                onClick = onNavigateToNotification,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = Color.White.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            if (notificationUnreadCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFFF1744),
                                    contentColor = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-2).dp, y = 2.dp)
                                ) {
                                    Text(
                                        if (notificationUnreadCount > 9) "9+" else notificationUnreadCount.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- TAB ROW ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTabIndex == index

                            val selectedBg = MaterialTheme.colorScheme.primary
                            val selectedContentColor = MaterialTheme.colorScheme.onPrimary
                            val unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .shadow(
                                                    elevation = 4.dp,
                                                    shape = RoundedCornerShape(24.dp),
                                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                )
                                                .background(
                                                    color = selectedBg,
                                                    shape = RoundedCornerShape(24.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(24.dp)
                                                )
                                        } else {
                                            Modifier
                                                .background(Color.Transparent)
                                                // Viền mờ cho tab chưa chọn
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(24.dp)
                                                )
                                        }
                                    )
                                    .clickable { selectedTabIndex = index }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (index == 0) Icons.Filled.Public else Icons.Filled.People,
                                        contentDescription = null,
                                        tint = if (isSelected) selectedContentColor else unselectedContentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = if (isSelected) selectedContentColor else unselectedContentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- SAVED FILTER BUTTON (Only Visible in "For You" Tab) ---
            AnimatedVisibility(
                visible = selectedTabIndex == 1,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .shadow(
                                elevation = if (isShowingSavedPosts) 8.dp else 2.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = if (isShowingSavedPosts) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                ambientColor = if (isShowingSavedPosts) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.1f)
                            )
                            .clickable { isShowingSavedPosts = !isShowingSavedPosts },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isShowingSavedPosts)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isShowingSavedPosts) 1.5.dp else 1.dp,
                            color = if (isShowingSavedPosts)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = if (isShowingSavedPosts) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = if (isShowingSavedPosts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = preferencesManager.getString("tab_saved"),
                                fontSize = 14.sp,
                                fontWeight = if (isShowingSavedPosts) FontWeight.Bold else FontWeight.Medium,
                                color = if (isShowingSavedPosts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // --- LIST LOGIC ---
            val displayedPosts = remember(selectedTabIndex, isShowingSavedPosts, savedPostIds.size, hiddenPostIds.size) {
                when(selectedTabIndex) {
                    0 -> samplePosts.filter { it.id !in hiddenPostIds } // Everyone
                    1 -> {
                        // For You logic
                        if (isShowingSavedPosts) {
                            // Only saved posts
                            samplePosts.filter { it.id in savedPostIds && it.id !in hiddenPostIds }
                        } else {
                            // All For You posts
                            samplePosts.filter { it.id !in hiddenPostIds }
                        }
                    }
                    else -> emptyList()
                }
            }

            if (displayedPosts.isEmpty() && isShowingSavedPosts && selectedTabIndex == 1) {
                // Empty State for Saved Posts
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 100.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = preferencesManager.getString("empty_saved_posts"),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp) // Add padding bottom for FAB/Nav
                ) {
                    items(
                        items = displayedPosts,
                        key = { it.id }
                    ) { post ->
                        PostCard(
                            post = post,
                            isSaved = post.id in savedPostIds,
                            onSaveClick = {
                                if (post.id in savedPostIds) {
                                    savedPostIds.remove(post.id)
                                    // If we are in "Saved" view and unsave, list will update automatically
                                } else {
                                    savedPostIds.add(post.id)
                                    snackbarMessage = preferencesManager.getString("post_saved")
                                    showSnackbar = true
                                }
                            },
                            onHideClick = {
                                hiddenPostIds.add(post.id)
                                snackbarMessage = preferencesManager.getString("post_hidden")
                                showSnackbar = true
                            },
                            onReportSubmitted = {
                                snackbarMessage = preferencesManager.getString("report_success")
                                showSnackbar = true
                            },
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
                    Text(snackbarMessage, fontWeight = FontWeight.Medium)
                }
            }

            LaunchedEffect(showSnackbar) {
                kotlinx.coroutines.delay(2000)
                showSnackbar = false
            }
        }

        // Comment bottom sheet
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

@Composable
fun PostCard(
    post: Post,
    isSaved: Boolean = false,
    onSaveClick: () -> Unit = {},
    onHideClick: () -> Unit = {},
    onReportSubmitted: () -> Unit = {},
    onCommentClick: () -> Unit = {}
) {
    var isLiked by remember { mutableStateOf(post.isLiked) }
    var likeCount by remember { mutableIntStateOf(post.likeCount) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(50.dp)
                                .shadow(6.dp, CircleShape),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = post.userName.first().toString(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = post.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = post.timeAgo,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Box {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .width(220.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = if (isSaved) {
                                                preferencesManager.getString("unsave_post")
                                            } else {
                                                preferencesManager.getString("save_post")
                                            },
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onSaveClick()
                                },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = preferencesManager.getString("hide_post"),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onHideClick()
                                },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = preferencesManager.getString("report_post"),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "${preferencesManager.getString("about_user")} ${post.userName}",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                if (showReportDialog) {
                    ReportDialog(
                        onDismiss = { showReportDialog = false },
                        onSubmit = { reason, description ->
                            onReportSubmitted()
                        },
                        preferencesManager = preferencesManager
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Post content
                Text(
                    text = post.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Location tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.location,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.outlineVariant,
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val likeBg = if (isLiked) MaterialTheme.colorScheme.errorContainer else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                val likeContentColor = if (isLiked) MaterialTheme.colorScheme.error else
                    MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = likeBg,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                isLiked = !isLiked
                                likeCount = if (isLiked) likeCount + 1 else likeCount - 1
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = likeContentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = likeCount.toString(),
                            color = likeContentColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { onCommentClick() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = post.commentCount.toString(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = { /* Handle share */ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}