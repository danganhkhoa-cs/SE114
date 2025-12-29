package com.example.se114.ui.presentation.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.se114.data.Post
import com.example.se114.local.PreferencesManager
import com.example.se114.utils.TimeUtils

@Composable
fun PostCard(
    post: Post,
    isSaved: Boolean,
    preferencesManager: PreferencesManager,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onReportClick: () -> Unit, // Đổi tên cho rõ ràng
    onCommentClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onNavigateToPostDetail(post.id) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), Color.Transparent)))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                    // Avatar & Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onAvatarClick() }
                    ) {
                        Surface(
                            modifier = Modifier.size(50.dp).shadow(6.dp, CircleShape),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (post.userAvatar.startsWith("http")) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(post.userAvatar).crossfade(true).build(),
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Text(
                                        text = if(post.userName.isNotEmpty()) post.userName.first().toString() else "?",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(post.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = TimeUtils.getTimeAgo(post.createdAt, preferencesManager),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Menu
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isSaved) preferencesManager.getString("unsave_post") else preferencesManager.getString("save_post")) },
                                onClick = { showMenu = false; onSaveClick() },
                                leadingIcon = { Icon(if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder, null, tint = MaterialTheme.colorScheme.primary) }
                            )
                            DropdownMenuItem(
                                text = { Text(preferencesManager.getString("report_post"), color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onReportClick() },
                                leadingIcon = { Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            // Content
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(post.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 21.sp)
                Spacer(modifier = Modifier.height(12.dp))
                // Location & Category Tags
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(8.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("${preferencesManager.getString(post.district)}, ${preferencesManager.getString(post.city)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(start = 4.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Label, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(preferencesManager.getString(post.category), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

            // Actions Buttons (Like, Comment, Share)
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                // Like Button
                TextButton(onClick = onLikeClick) {
                    Icon(
                        if (post.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(post.likeCount.toString(), color = if (post.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Comment Button
                TextButton(onClick = onCommentClick) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(post.commentCount.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Share Button
                IconButton(onClick = {
                    // 1. Tạo link (giống format đã khai báo trong Manifest & Navigation)
                    val deepLinkUrl = "https://locasos.com/post/${post.id}"

                    // 2. Tạo Intent chia sẻ hệ thống
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, deepLinkUrl)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "")
                    context.startActivity(shareIntent)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}