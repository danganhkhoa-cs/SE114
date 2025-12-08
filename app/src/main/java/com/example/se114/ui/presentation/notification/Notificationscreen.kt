package com.example.se114.ui.presentation.notification

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.se114.local.PreferencesManager

enum class NotificationType {
    LIKE,
    COMMENT,
    REPLY,
    FRIEND_REQUEST,
    SOS_SUPPORT_ACCEPTED,
    EMERGENCY_APPROVED,
    EMERGENCY_REJECTED
}

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val userName: String,
    val userAvatar: String? = null,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val postId: String? = null,
    val requestId: String? = null
)

enum class NotificationTab {
    SOCIAL,
    EMERGENCY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    preferencesManager: PreferencesManager
) {
    // Sample data - replace with real data from ViewModel
    val socialNotifications = remember {
        mutableStateListOf(
            NotificationItem(
                id = "1",
                type = NotificationType.LIKE,
                userName = "Nguyễn Văn A",
                message = "liked your post",
                timestamp = System.currentTimeMillis() - 3600000,
                isRead = false
            ),
            NotificationItem(
                id = "2",
                type = NotificationType.COMMENT,
                userName = "Trần Thị B",
                message = "commented on your post: 'Great content!'",
                timestamp = System.currentTimeMillis() - 7200000,
                isRead = false
            ),
            NotificationItem(
                id = "3",
                type = NotificationType.FRIEND_REQUEST,
                userName = "Lê Văn C",
                message = "sent you a friend request",
                timestamp = System.currentTimeMillis() - 86400000,
                isRead = true
            ),
            NotificationItem(
                id = "4",
                type = NotificationType.REPLY,
                userName = "Phạm Thị D",
                message = "replied to your comment",
                timestamp = System.currentTimeMillis() - 172800000,
                isRead = true
            )
        )
    }

    val emergencyNotifications = remember {
        mutableStateListOf(
            NotificationItem(
                id = "5",
                type = NotificationType.SOS_SUPPORT_ACCEPTED,
                userName = "Rescue Team Alpha",
                message = "accepted to support your SOS post",
                timestamp = System.currentTimeMillis() - 1800000,
                isRead = false
            ),
            NotificationItem(
                id = "6",
                type = NotificationType.EMERGENCY_APPROVED,
                userName = "Emergency Control Center",
                message = "Your emergency request has been approved",
                timestamp = System.currentTimeMillis() - 5400000,
                isRead = false
            ),
            NotificationItem(
                id = "7",
                type = NotificationType.EMERGENCY_REJECTED,
                userName = "Emergency Control Center",
                message = "Your emergency request requires more information",
                timestamp = System.currentTimeMillis() - 259200000,
                isRead = true
            )
        )
    }

    var selectedTab by remember { mutableStateOf(NotificationTab.SOCIAL) }

    val socialUnreadCount = socialNotifications.count { !it.isRead }
    val emergencyUnreadCount = emergencyNotifications.count { !it.isRead }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        preferencesManager.getString("notifications"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Mark all as read button
                    Surface(
                        onClick = {
                            if (selectedTab == NotificationTab.SOCIAL) {
                                socialNotifications.replaceAll { it.copy(isRead = true) }
                            } else {
                                emergencyNotifications.replaceAll { it.copy(isRead = true) }
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Mark all as read",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = preferencesManager.getString("mark_all_read"),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 8.dp)
                    .height(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple(NotificationTab.SOCIAL, preferencesManager.getString("social"), Icons.Default.People),
                    Triple(NotificationTab.EMERGENCY, preferencesManager.getString("emergency"), Icons.Default.Warning)
                ).forEachIndexed { index, (tab, title, icon) ->
                    val isSelected = selectedTab == tab
                    val unreadCount = if (tab == NotificationTab.SOCIAL) socialUnreadCount else emergencyUnreadCount

                    val selectedBg = MaterialTheme.colorScheme.primary
                    val selectedContentColor = MaterialTheme.colorScheme.onPrimary
                    val unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Tab button
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
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
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(24.dp)
                                            )
                                    }
                                )
                                .clickable { selectedTab = tab },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = if (isSelected) selectedContentColor else unselectedContentColor,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = title,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) selectedContentColor else unselectedContentColor
                                )
                            }
                        }

                        // Badge on top right corner of tab (outside clickable area)
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .background(
                                        color = Color(0xFFE53935),
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 2.5.dp,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Clear All Button (below tabs)
            if ((selectedTab == NotificationTab.SOCIAL && socialNotifications.isNotEmpty()) ||
                (selectedTab == NotificationTab.EMERGENCY && emergencyNotifications.isNotEmpty())) {

                Surface(
                    onClick = {
                        if (selectedTab == NotificationTab.SOCIAL) {
                            socialNotifications.clear()
                        } else {
                            emergencyNotifications.clear()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear all",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = preferencesManager.getString("clear_all"),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Notification List
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "tab_transition",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                val notifications = if (tab == NotificationTab.SOCIAL) {
                    socialNotifications
                } else {
                    emergencyNotifications
                }

                if (notifications.isEmpty()) {
                    EmptyNotificationState(
                        tab = tab,
                        preferencesManager = preferencesManager
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        items(
                            items = notifications,
                            key = { it.id }
                        ) { notification ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = RoundedCornerShape(18.dp),
                                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    ),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surface,
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                )
                                            )
                                        )
                                ) {
                                    NotificationCard(
                                        notification = notification,
                                        preferencesManager = preferencesManager,
                                        onClick = {
                                            val index = notifications.indexOf(notification)
                                            if (index != -1) {
                                                notifications[index] = notification.copy(isRead = true)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    preferencesManager: PreferencesManager,
    onClick: () -> Unit
) {
    val iconData = getNotificationIconData(notification.type)
    val timeAgo = getTimeAgo(notification.timestamp, preferencesManager)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (notification.isRead) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = iconData.backgroundColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconData.icon,
                    contentDescription = null,
                    tint = iconData.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = getLocalizedMessage(notification, preferencesManager),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = timeAgo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )

                // Friend request action buttons
                if (notification.type == NotificationType.FRIEND_REQUEST) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Accept button
                        Surface(
                            onClick = {
                                // Handle accept friend request
                                onClick()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            tonalElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = preferencesManager.getString("friend_accept"),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Reject button
                        Surface(
                            onClick = {
                                // Handle reject friend request
                                onClick()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.error,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = preferencesManager.getString("friend_reject"),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotificationState(
    tab: NotificationTab,
    preferencesManager: PreferencesManager
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (tab == NotificationTab.SOCIAL) {
                        Icons.Default.Notifications
                    } else {
                        Icons.Default.NotificationsActive
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = preferencesManager.getString("no_notifications"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (tab == NotificationTab.SOCIAL) {
                    preferencesManager.getString("social_empty_msg")
                } else {
                    preferencesManager.getString("emergency_empty_msg")
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

data class NotificationIconData(
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color
)

@Composable
fun getNotificationIconData(type: NotificationType): NotificationIconData {
    return when (type) {
        NotificationType.LIKE -> NotificationIconData(
            icon = Icons.Default.Favorite,
            iconColor = Color(0xFFE91E63),
            backgroundColor = Color(0xFFE91E63).copy(alpha = 0.15f)
        )
        NotificationType.COMMENT -> NotificationIconData(
            icon = Icons.Default.Comment,
            iconColor = Color(0xFF2196F3),
            backgroundColor = Color(0xFF2196F3).copy(alpha = 0.15f)
        )
        NotificationType.REPLY -> NotificationIconData(
            icon = Icons.Default.Reply,
            iconColor = Color(0xFF9C27B0),
            backgroundColor = Color(0xFF9C27B0).copy(alpha = 0.15f)
        )
        NotificationType.FRIEND_REQUEST -> NotificationIconData(
            icon = Icons.Default.PersonAdd,
            iconColor = Color(0xFF4CAF50),
            backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
        )
        NotificationType.SOS_SUPPORT_ACCEPTED -> NotificationIconData(
            icon = Icons.Default.CheckCircle,
            iconColor = Color(0xFF4CAF50),
            backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
        )
        NotificationType.EMERGENCY_APPROVED -> NotificationIconData(
            icon = Icons.Default.Verified,
            iconColor = Color(0xFF00BCD4),
            backgroundColor = Color(0xFF00BCD4).copy(alpha = 0.15f)
        )
        NotificationType.EMERGENCY_REJECTED -> NotificationIconData(
            icon = Icons.Default.Cancel,
            iconColor = Color(0xFFFF5722),
            backgroundColor = Color(0xFFFF5722).copy(alpha = 0.15f)
        )
    }
}

fun getLocalizedMessage(notification: NotificationItem, preferencesManager: PreferencesManager): String {
    return when (notification.type) {
        NotificationType.LIKE -> {
            preferencesManager.getString("notif_liked_post")
        }
        NotificationType.COMMENT -> {
            val comment = notification.message.substringAfter(": '").substringBefore("'")
            "${preferencesManager.getString("notif_commented")}: '$comment'"
        }
        NotificationType.REPLY -> {
            preferencesManager.getString("notif_replied")
        }
        NotificationType.FRIEND_REQUEST -> {
            preferencesManager.getString("notif_friend_request")
        }
        NotificationType.SOS_SUPPORT_ACCEPTED -> {
            preferencesManager.getString("notif_sos_accepted")
        }
        NotificationType.EMERGENCY_APPROVED -> {
            preferencesManager.getString("notif_emergency_approved")
        }
        NotificationType.EMERGENCY_REJECTED -> {
            preferencesManager.getString("notif_emergency_rejected")
        }
    }
}

fun getTimeAgo(timestamp: Long, preferencesManager: PreferencesManager): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = days / 30

    return when {
        seconds < 60 -> preferencesManager.getString("time_just_now")
        minutes < 60 -> "$minutes ${preferencesManager.getString("time_minutes_ago")}"
        hours < 24 -> "$hours ${preferencesManager.getString("time_hours_ago")}"
        days < 7 -> "$days ${preferencesManager.getString("time_days_ago")}"
        weeks < 4 -> "$weeks ${preferencesManager.getString("time_weeks_ago")}"
        else -> "$months ${preferencesManager.getString("time_months_ago")}"
    }
}