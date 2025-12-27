package com.example.se114.ui.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.se114.data.Comment
import com.example.se114.local.PreferencesManager
import com.example.se114.utils.TimeUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    onDismiss: () -> Unit,
    postId: String,
    preferencesManager: PreferencesManager,
    viewModel: CommentViewModel = hiltViewModel() // Inject ViewModel
) {
    // 1. Load data khi mở Sheet
    LaunchedEffect(postId) {
        viewModel.loadComments(postId)
    }

    // 2. Collect State từ ViewModel
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var offsetY by remember { mutableStateOf(0f) }
    val maxDragDistance = 300f
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Quản lý Input Reply
    // Map lưu text reply cho từng comment cha (Key: CommentID string)
    var replyTextMap by remember { mutableStateOf(mutableMapOf<String, String>()) }
    // ID của comment đang được reply
    var replyingToId by remember { mutableStateOf<String?>(null) }

    // Quản lý Input chính
    var mainCommentText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * (1 - offsetY / maxDragDistance).coerceIn(0f, 1f)))
                .clickable(onClick = onDismiss)
        )

        // Sheet Content
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (offsetY > maxDragDistance / 2) onDismiss() else offsetY = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                        }
                    )
                },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag Handle
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.width(40.dp).height(4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
                }

                // Title
                Text(
                    text = preferencesManager.getString("comments"),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // --- LIST COMMENTS ---
                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (comments.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(preferencesManager.getString("no_comments"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp) // Chừa chỗ cho input bar
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            // 1. Render Root Comment
                            CommentItem(
                                comment = comment,
                                preferencesManager = preferencesManager,
                                isReply = false,
                                onLikeClick = { viewModel.toggleLike(comment) },
                                onReplyClick = { replyingToId = comment.id }
                            )

                            // 2. Render Inline Reply Input (Nếu đang reply cho comment này)
                            if (replyingToId == comment.id) {
                                InlineReplyInput(
                                    replyText = replyTextMap[comment.id] ?: "",
                                    onReplyTextChange = { txt ->
                                        replyTextMap = replyTextMap.toMutableMap().apply { put(comment.id, txt) }
                                    },
                                    onSendReply = {
                                        val content = replyTextMap[comment.id] ?: ""
                                        if (content.isNotBlank()) {
                                            viewModel.sendComment(content, parentId = comment.id)
                                            // Reset input
                                            replyTextMap.remove(comment.id)
                                            replyingToId = null
                                        }
                                    },
                                    onCancel = { replyingToId = null },
                                    preferencesManager = preferencesManager
                                )
                            }

                            // 3. Render Replies List
                            comment.replies.forEach { reply ->
                                CommentItem(
                                    comment = reply,
                                    preferencesManager = preferencesManager,
                                    isReply = true, // UI thụt đầu dòng
                                    onLikeClick = { viewModel.toggleLike(reply) },
                                    onReplyClick = {
                                        // Logic 1 cấp: Reply cho con thì trỏ về cha
                                        replyingToId = comment.id
                                    }
                                )
                            }
                        }
                    }
                }

                // --- MAIN INPUT BAR (Bottom) ---
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar (Current User)
                        val myAvatar = preferencesManager.userAvatar
                        if (myAvatar.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(myAvatar).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                            )
                        } else {
                            Box(modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = mainCommentText,
                            onValueChange = { mainCommentText = it },
                            placeholder = { Text(preferencesManager.getString("write_comment"), fontSize = 14.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )

                        IconButton(
                            onClick = {
                                viewModel.sendComment(mainCommentText, parentId = null) // Send Root
                                mainCommentText = ""
                                // Scroll lên đầu hoặc cuối tùy logic (ở đây realtime sẽ tự update list)
                                coroutineScope.launch { listState.animateScrollToItem(0) } // Giả sử list mới nhất lên đầu (hoặc bạn đổi logic sort)
                            },
                            enabled = mainCommentText.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (mainCommentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
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
    isReply: Boolean,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isReply) 56.dp else 16.dp, // Thụt lề nếu là Reply
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            )
    ) {
        // 1. Avatar (Giữ nguyên)
        Box(modifier = Modifier.size(if (isReply) 32.dp else 40.dp)) {
            if (comment.userAvatar.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(comment.userAvatar).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if(comment.userName.isNotEmpty()) comment.userName.first().toString() else "?",
                        color = Color.White,
                        fontSize = if(isReply) 12.sp else 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // 2. Tên & Nội dung (Giữ nguyên)
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Text(comment.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(comment.content, fontSize = 14.sp, lineHeight = 18.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Actions (Time - Like - Reply)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                // A. Thời gian (Dùng TimeUtils)
                Text(
                    text = TimeUtils.getTimeAgo(comment.timestamp, preferencesManager),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // B. Nút Like (ICON TRÁI TIM + SỐ LƯỢNG)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp), // Khoảng cách giữa tim và số
                    modifier = Modifier.clickable(onClick = onLikeClick)
                ) {
                    // Icon Trái tim
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        // Nếu đã like -> Màu đỏ (Error), chưa like -> Màu xám
                        tint = if (comment.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )

                    // Số lượng Like
                    Text(
                        text = "${comment.likeCount}",
                        fontSize = 12.sp,
                        fontWeight = if(comment.isLiked) FontWeight.Bold else FontWeight.Medium,
                        color = if(comment.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // C. Nút Reply (Giữ nguyên)
                if (!isReply) {
                    Text(
                        text = preferencesManager.getString("reply"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(onClick = onReplyClick)
                    )
                }
            }
        }
    }
}

@Composable
fun InlineReplyInput(
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onSendReply: () -> Unit,
    onCancel: () -> Unit,
    preferencesManager: PreferencesManager
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Reply, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(preferencesManager.getString("reply"), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onCancel, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = onReplyTextChange,
                    placeholder = { Text(preferencesManager.getString("write_comment"), fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onSendReply, enabled = replyText.isNotBlank(), modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}