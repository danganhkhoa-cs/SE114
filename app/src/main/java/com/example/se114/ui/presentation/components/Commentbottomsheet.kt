package com.example.se114.ui.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.se114.local.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class Comment(
    val id: Int,
    val userName: String,
    val userAvatar: String = "",
    val content: String,
    val timestamp: Long,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val replies: List<Comment> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    onDismiss: () -> Unit,
    postId: Int,
    preferencesManager: PreferencesManager
) {
    var offsetY by remember { mutableStateOf(0f) }
    val maxDragDistance = 300f
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Get keyboard visibility and height
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val imeHeight = WindowInsets.ime.getBottom(LocalDensity.current)

    // Auto scroll when keyboard appears and there's a reply input active
    var replyingToId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(imeVisible, replyingToId) {
        if (imeVisible && replyingToId != null) {
            // Keyboard is open and we're replying - scroll to keep reply input visible
            delay(100) // Small delay for animation
            // No need to scroll, imePadding handles it
        }
    }

    // Sample comments data
    val comments = remember {
        mutableStateListOf(
            Comment(
                id = 1,
                userName = "Nguyễn Văn A",
                content = "Bài viết rất hay và bổ ích!",
                timestamp = System.currentTimeMillis() - 3600000,
                likeCount = 12,
                isLiked = false
            ),
            Comment(
                id = 2,
                userName = "Trần Thị B",
                content = "Cảm ơn bạn đã chia sẻ 😊",
                timestamp = System.currentTimeMillis() - 7200000,
                likeCount = 5,
                isLiked = true,
                replies = listOf(
                    Comment(
                        id = 21,
                        userName = "Admin",
                        content = "Cảm ơn bạn đã quan tâm!",
                        timestamp = System.currentTimeMillis() - 3600000,
                        likeCount = 2
                    )
                )
            ),
            Comment(
                id = 3,
                userName = "Lê Văn C",
                content = "Mình có thể hỏi thêm về vấn đề này không?",
                timestamp = System.currentTimeMillis() - 10800000,
                likeCount = 3
            )

        )
    }

    var mainCommentText by remember { mutableStateOf("") }
    var replyTexts by remember { mutableStateOf(mutableMapOf<Int, String>()) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Dimmed background - clickable to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * (1 - offsetY / maxDragDistance).coerceIn(0f, 1f)))
                .clickable(
                    onClick = onDismiss,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                )
        )

        // Comment sheet
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (offsetY > maxDragDistance / 2) {
                                onDismiss()
                            } else {
                                offsetY = 0f
                            }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            val newOffset = (offsetY + dragAmount).coerceAtLeast(0f)
                            offsetY = newOffset
                        }
                    )
                },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preferencesManager.getString("comments"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                // Comments list
                if (comments.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = preferencesManager.getString("no_comments"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = preferencesManager.getString("be_first_comment"),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            top = 12.dp,
                            bottom = 12.dp
                        )
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            CommentItem(
                                comment = comment,
                                onLikeClick = {
                                    val index = comments.indexOf(comment)
                                    if (index != -1) {
                                        comments[index] = comment.copy(
                                            isLiked = !comment.isLiked,
                                            likeCount = if (comment.isLiked) comment.likeCount - 1 else comment.likeCount + 1
                                        )
                                    }
                                },
                                onReplyClick = {
                                    replyingToId = comment.id
                                },
                                onReplyLikeClick = { replyId ->
                                    // Handle like on reply
                                    val index = comments.indexOf(comment)
                                    if (index != -1) {
                                        val updatedReplies = comment.replies.map { reply ->
                                            if (reply.id == replyId) {
                                                reply.copy(
                                                    isLiked = !reply.isLiked,
                                                    likeCount = if (reply.isLiked) reply.likeCount - 1 else reply.likeCount + 1
                                                )
                                            } else reply
                                        }
                                        comments[index] = comment.copy(replies = updatedReplies)
                                    }
                                },
                                preferencesManager = preferencesManager
                            )

                            // Inline reply input (appears below clicked comment)
                            if (replyingToId == comment.id) {
                                InlineReplyInput(
                                    replyText = replyTexts[comment.id] ?: "",
                                    onReplyTextChange = { newText ->
                                        replyTexts = replyTexts.toMutableMap().apply {
                                            put(comment.id, newText)
                                        }
                                    },
                                    onSendReply = {
                                        val text = replyTexts[comment.id] ?: ""
                                        if (text.isNotBlank()) {
                                            // Create new reply
                                            val newReply = Comment(
                                                id = (comments.maxOfOrNull { it.id } ?: 0) + 100,
                                                userName = "You",
                                                content = text,
                                                timestamp = System.currentTimeMillis(),
                                                likeCount = 0,
                                                isLiked = false
                                            )

                                            // Update comment with new reply
                                            val index = comments.indexOf(comment)
                                            if (index != -1) {
                                                val updatedReplies = comment.replies.toMutableList()
                                                updatedReplies.add(newReply)
                                                comments[index] = comment.copy(replies = updatedReplies)
                                            }

                                            replyTexts = replyTexts.toMutableMap().apply { remove(comment.id) }
                                            replyingToId = null
                                        }
                                    },
                                    onCancel = {
                                        replyTexts = replyTexts.toMutableMap().apply { remove(comment.id) }
                                        replyingToId = null
                                    },
                                    preferencesManager = preferencesManager
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                // Main comment input (at bottom)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),

                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar placeholder
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Text field
                        OutlinedTextField(
                            value = mainCommentText,
                            onValueChange = { mainCommentText = it },
                            placeholder = {
                                Text(
                                    preferencesManager.getString("write_comment"),
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            maxLines = 4
                        )

                        // Send button
                        IconButton(
                            onClick = {
                                if (mainCommentText.isNotBlank()) {
                                    // Add main comment to list
                                    val newComment = Comment(
                                        id = (comments.maxOfOrNull { it.id } ?: 0) + 1,
                                        userName = "You", // Current user
                                        content = mainCommentText,
                                        timestamp = System.currentTimeMillis(),
                                        likeCount = 0,
                                        isLiked = false
                                    )
                                    comments.add(0, newComment) // Add to top
                                    mainCommentText = ""

                                    // Scroll to top to see new comment
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                }
                            },
                            enabled = mainCommentText.isNotBlank()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = preferencesManager.getString("send"),
                                tint = if (mainCommentText.isNotBlank()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                        }
                    }
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
            .padding(start = 52.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Reply,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = preferencesManager.getString("reply"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = onReplyTextChange,
                    placeholder = {
                        Text(
                            preferencesManager.getString("write_comment"),
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    maxLines = 3,
                    minLines = 1
                )

                IconButton(
                    onClick = onSendReply,
                    enabled = replyText.isNotBlank(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = preferencesManager.getString("send"),
                        tint = if (replyText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    onReplyLikeClick: (Int) -> Unit = {},
    preferencesManager: PreferencesManager,
    isReply: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isReply) 48.dp else 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Username
                Text(
                    text = comment.userName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Comment content
                Text(
                    text = comment.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable(onClick = onLikeClick)
                    ) {
                        Icon(
                            imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (comment.isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        if (comment.likeCount > 0) {
                            Text(
                                text = "${comment.likeCount}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Reply button
                    if (!isReply) {
                        Text(
                            text = preferencesManager.getString("reply"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable(onClick = onReplyClick)
                        )
                    }

                    // Time
                    Text(
                        text = getTimeAgo(comment.timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Replies
        if (comment.replies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            comment.replies.forEach { reply ->
                CommentItem(
                    comment = reply,
                    onLikeClick = { onReplyLikeClick(reply.id) },
                    onReplyClick = { /* Replies can't have replies */ },
                    onReplyLikeClick = {},
                    preferencesManager = preferencesManager,
                    isReply = true
                )
            }
        }
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        else -> "${days}d"
    }
}