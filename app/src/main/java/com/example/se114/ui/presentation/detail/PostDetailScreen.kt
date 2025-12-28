package com.example.se114.ui.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.se114.data.Comment
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.components.PostCard
import com.example.se114.ui.presentation.components.PostEventListener
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.utils.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    viewModel: PostDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val post by viewModel.post.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val commentText by viewModel.commentText.collectAsStateWithLifecycle()
    val replyingTo by viewModel.replyingTo.collectAsStateWithLifecycle() // Lấy trạng thái reply
    val preferencesManager = viewModel.preferencesManager

    PostEventListener(viewModel = viewModel) { onLike, onSave, onReport, _ ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(preferencesManager.getString("post_details") ?: "Post Detail", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTealDark)
                )
            },
            bottomBar = {
                // Thanh chat nâng cấp: Hiển thị banner khi đang reply
                CommentInputArea(
                    text = commentText,
                    replyingTo = replyingTo,
                    onTextChanged = viewModel::onCommentTextChanged,
                    onSendClick = viewModel::sendComment,
                    onCancelReply = viewModel::onCancelReply,
                    preferencesManager = preferencesManager
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (post == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // 1. Post Content
                        item {
                            PostCard(
                                post = post!!,
                                isSaved = false,
                                preferencesManager = preferencesManager,
                                onLikeClick = { onLike(post!!) },
                                onSaveClick = { onSave(post!!, false) },
                                onReportClick = { onReport(post!!.id) },
                                onCommentClick = { },
                                onAvatarClick = { },
                                onNavigateToPostDetail = { }
                            )
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Đếm tổng comment (Cha + Con)
                            val totalComments = comments.sumOf { 1 + it.replies.size }
                            Text(
                                text = "${preferencesManager.getString("comments")} ($totalComments)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 2. Comments List (Render cả cha và con)
                        items(comments) { comment ->
                            // Render Root Comment
                            CommentItem(
                                comment = comment,
                                preferencesManager = preferencesManager,
                                isReply = false,
                                onLikeClick = { viewModel.onToggleLikeComment(comment) },
                                onReplyClick = { viewModel.onReplyToComment(comment) }
                            )

                            // Render Replies (Thụt vào)
                            comment.replies.forEach { reply ->
                                CommentItem(
                                    comment = reply,
                                    preferencesManager = preferencesManager,
                                    isReply = true,
                                    onLikeClick = { viewModel.onToggleLikeComment(reply) },
                                    // Logic: Reply cho con thì trỏ về cha (để gom nhóm)
                                    onReplyClick = { viewModel.onReplyToComment(comment) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    preferencesManager: PreferencesManager,
    isReply: Boolean, // Biến này xác định đây là comment con hay cha
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit
) {
    val context = LocalContext.current

    // Tính toán padding: Reply thì thụt vào sâu hơn
    val startPadding = if (isReply) 56.dp else 16.dp
    val avatarSize = if (isReply) 32.dp else 40.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        // --- 1. AVATAR ---
        if (comment.userAvatar.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(comment.userAvatar).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(avatarSize).clip(CircleShape)
            )
        } else {
            Surface(
                modifier = Modifier.size(avatarSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = comment.userName.firstOrNull()?.toString() ?: "?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = if(isReply) 12.sp else 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // --- 2. NỘI DUNG & ACTIONS ---
        Column {
            // Bubble chứa Tên & Nội dung
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.wrapContentWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = comment.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = comment.content,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Row: Time - Like - Reply
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                // A. Thời gian
                Text(
                    text = TimeUtils.getTimeAgo(comment.timestamp, preferencesManager),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // B. Nút Like (Luôn hiện ở cả cha và con)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onLikeClick)
                ) {
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (comment.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (comment.likeCount > 0) {
                        Text(
                            text = "${comment.likeCount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if(comment.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // C. Nút Reply (CHỈ HIỆN NẾU LÀ COMMENT GỐC)
                if (!isReply) {
                    Text(
                        text = preferencesManager.getString("reply") ?: "Reply",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(onClick = onReplyClick)
                    )
                }
            }
        }
    }
}

// --- UI THANH NHẬP LIỆU (Có Banner Reply) ---
@Composable
fun CommentInputArea(
    text: String,
    replyingTo: Comment?,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onCancelReply: () -> Unit,
    preferencesManager: PreferencesManager
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp, // Đổ bóng cao hơn chút để tách biệt
        tonalElevation = 2.dp
    ) {
        Column {
            // Banner hiển thị "Đang trả lời..."
            if (replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${preferencesManager.getString("replying_to") ?: "Replying to"} ${replyingTo.userName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Thanh nhập liệu
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    placeholder = {
                        Text(if(replyingTo == null) preferencesManager.getString("write_comment") ?: "Write a comment..." else "Reply...")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Transparent),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSendClick,
                    enabled = text.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (text.isNotBlank()) AppTealDark else Color.Gray.copy(alpha = 0.3f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }
    }
}