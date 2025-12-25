package com.example.se114.ui.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.se114.data.Post
import com.example.se114.local.PreferencesManager

@Composable
fun PostFeed(
    posts: List<Post>,
    savedPostIds: Set<String>, // Để check bài nào đã save
    preferencesManager: PreferencesManager,
    onLikeClick: (String) -> Unit, // Chỉ truyền ID
    onSaveClick: (String) -> Unit,
    onHideClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onCommentClick: (Post) -> Unit, // Truyền cả Post để mở bottom sheet
    onNavigateToOtherProfile: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    currentUserId: String,
    emptyMessage: String = "No posts found"
) {
    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = emptyMessage, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(items = posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    isSaved = savedPostIds.contains(post.id), // Logic check saved nằm ở đây
                    preferencesManager = preferencesManager,
                    onLikeClick = { onLikeClick(post.id) },
                    onSaveClick = { onSaveClick(post.id) },
                    onHideClick = { onHideClick(post.id) },
                    onReportClick = { onReportClick(post.id) },
                    onCommentClick = { onCommentClick(post) },
                    onAvatarClick = {
                        if (post.userId == currentUserId) {
                            onNavigateToProfile()
                        } else {
                            onNavigateToOtherProfile(post.userId)
                        }
                    }
                )
            }
        }
    }
}