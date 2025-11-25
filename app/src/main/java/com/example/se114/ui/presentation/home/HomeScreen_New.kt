package com.example.se114.ui.presentation.home

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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.AppTealLight

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
fun HomeScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Everyone", "For You")

    // Sample posts data
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
            .background(AppTealLight)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar with notification icon
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
                    // Decorative blob 1
                    Canvas(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 30.dp, y = (-20).dp)
                    ) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f),
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
                            color = Color.White.copy(alpha = 0.08f),
                            radius = size.minDimension / 2
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = 50.dp, y = 15.dp)
                    ) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.06f),
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

                            Column {
                                Text(
                                    text = "LocaSOS",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 1.2.sp,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color.Black.copy(alpha = 0.25f),
                                            offset = androidx.compose.ui.geometry.Offset(1f, 2f),
                                            blurRadius = 3f
                                        )
                                    )
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { /* Navigate to notifications */ },
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

                            Badge(
                                containerColor = Color(0xFFFF1744),
                                contentColor = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-2).dp, y = 2.dp)
                            ) {
                                Text(
                                    "3",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Tab Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(
                                color = AppTealLight.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        color = if (selectedTabIndex == index) {
                                            AppTealDark
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = RoundedCornerShape(22.dp)
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
                                        tint = if (selectedTabIndex == index) Color.White else AppTealDark.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = if (selectedTabIndex == index) Color.White else AppTealDark.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Posts List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTealLight),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(samplePosts) { post ->
                    PostCard(post = post)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PostCard(post: Post) {
    var isLiked by remember { mutableStateOf(post.isLiked) }
    var likeCount by remember { mutableIntStateOf(post.likeCount) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(2.dp, AppTealDark.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                AppTealLight.copy(alpha = 0.2f),
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
                            color = AppTealDark,
                            border = BorderStroke(3.dp, Color.White)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = post.userName.first().toString(),
                                    color = Color.White,
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
                                color = Color.Black
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.Gray
                                )
                                Text(
                                    text = post.timeAgo,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.06f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(
                            onClick = { /* More options */ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = post.content,
                    fontSize = 14.sp,
                    color = Color.Black.copy(alpha = 0.9f),
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Location tag with better design
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = AppTealLight.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = AppTealDark.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = AppTealDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.location,
                        fontSize = 13.sp,
                        color = AppTealDark,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                AppTealDark.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isLiked) Color(0xFFFFEBEE) else Color(0xFFF8F8F8),
                    border = BorderStroke(
                        1.5.dp,
                        if (isLiked) Color(0xFFE53935).copy(alpha = 0.3f) else Color(0xFFE0E0E0)
                    ),
                    modifier = Modifier.weight(1f),
                    shadowElevation = if (isLiked) 2.dp else 0.dp
                ) {
                    TextButton(
                        onClick = {
                            isLiked = !isLiked
                            likeCount = if (isLiked) likeCount + 1 else likeCount - 1
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isLiked) Color(0xFFE53935) else Color.Gray
                        )
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$likeCount",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8F8F8),
                    border = BorderStroke(1.5.dp, Color(0xFFE0E0E0)),
                    modifier = Modifier.weight(1f)
                ) {
                    TextButton(
                        onClick = { /* Navigate to comments */ },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = AppTealDark
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubbleOutline,
                            contentDescription = "Comment",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.commentCount}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8F8F8),
                    border = BorderStroke(1.5.dp, Color(0xFFE0E0E0)),
                    modifier = Modifier.weight(1f)
                ) {
                    TextButton(
                        onClick = { /* Share post */ },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Gray
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}