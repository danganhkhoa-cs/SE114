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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.local.PreferencesManager
import com.example.se114.utils.TimeUtils
import com.example.se114.utils.TimeUtils.getTimeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateToOtherProfile: (String) -> Unit,
    onBackClick: () -> Unit,
    preferencesManager: PreferencesManager,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val socialUnreadCount = uiState.socialNotifications.count { !it.isRead }
    val emergencyUnreadCount = uiState.systemNotifications.count { !it.isRead }

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
                        onClick = viewModel::markAllAsRead,
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, "Mark all as read", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                            Text(
                                text = preferencesManager.getString("mark_all_read"),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )

        },
        bottomBar = {
            Spacer(Modifier.height(0.dp))
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 8.dp)
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple(NotificationTab.SOCIAL, preferencesManager.getString("social"), Icons.Default.People),
                    Triple(NotificationTab.SYSTEM, preferencesManager.getString("system"), Icons.Default.Warning)
                ).forEach { (tab, title, icon) ->
                    val isSelected = uiState.selectedTab == tab
                    val unreadCount = if (tab == NotificationTab.SOCIAL) socialUnreadCount else emergencyUnreadCount
                    val selectedBg = MaterialTheme.colorScheme.primary
                    val selectedContentColor = MaterialTheme.colorScheme.onPrimary
                    val unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isSelected) Modifier
                                        .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        .background(selectedBg, RoundedCornerShape(24.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                                    else Modifier
                                        .background(Color.Transparent)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                )
                                .clickable { viewModel.onTabSelected(tab) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, title, tint = if (isSelected) selectedContentColor else unselectedContentColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(title, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) selectedContentColor else unselectedContentColor)
                            }
                        }
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp).offset(x = 6.dp, y = (-6).dp)
                                    .background(Color(0xFFE53935), CircleShape)
                                    .border(2.5.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (unreadCount > 9) "9+" else unreadCount.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Notification List
            AnimatedContent(
                targetState = uiState.selectedTab,
                transitionSpec = { slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut() },
                label = "tab_transition",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                val notifications = if (tab == NotificationTab.SOCIAL) uiState.socialNotifications else uiState.systemNotifications

                if (notifications.isEmpty()) {
                    EmptyNotificationState(tab = tab, preferencesManager = preferencesManager)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                        items(items = notifications, key = { it.id }) { notification ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(18.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))))) {
                                    NotificationCard(
                                        notification = notification,
                                        preferencesManager = preferencesManager,
                                        onClick = {
                                            viewModel.markItemAsRead(notification)

                                            notification.senderId?.let { id ->
                                                onNavigateToOtherProfile(id)
                                            }
                                        },
                                        onAccept = { viewModel.acceptFriendRequest(notification) },
                                        onReject = { viewModel.rejectFriendRequest(notification) }
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(4.dp)) }
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
    onClick: () -> Unit,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {}
) {
    val iconData = getNotificationIconData(notification.type)
    val timeAgo = TimeUtils.getTimeAgo(notification.timestamp, preferencesManager)

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (notification.isRead) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(48.dp).background(iconData.backgroundColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(iconData.icon, null, tint = iconData.iconColor, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(preferencesManager.getString(notification.senderName), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    if (!notification.isRead) {
                        Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(getLocalizedMessage(notification, preferencesManager), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(timeAgo, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)

                if (notification.type == NotificationType.FRIEND_REQUEST) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(onClick = onAccept, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary, tonalElevation = 2.dp) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), CircleShape).border(1.5.dp, MaterialTheme.colorScheme.onPrimary, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, preferencesManager.getString("friend_accept"), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Surface(onClick = onReject, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape).border(1.5.dp, MaterialTheme.colorScheme.error, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Close, preferencesManager.getString("friend_reject"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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
fun EmptyNotificationState(tab: NotificationTab, preferencesManager: PreferencesManager) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(120.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (tab == NotificationTab.SOCIAL) Icons.Default.Notifications else Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(56.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(preferencesManager.getString("no_notifications"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (tab == NotificationTab.SOCIAL) preferencesManager.getString("social_empty_msg") else preferencesManager.getString("emergency_empty_msg"), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 20.sp)
        }
    }
}

// --- HELPER FUNCTIONS & DATA (Giữ lại ở Screen) ---

data class NotificationIconData(val icon: ImageVector, val iconColor: Color, val backgroundColor: Color)

@Composable
fun getNotificationIconData(type: NotificationType): NotificationIconData {
    return when (type) {
        NotificationType.LIKE -> NotificationIconData(Icons.Default.Favorite, Color(0xFFE91E63), Color(0xFFE91E63).copy(alpha = 0.15f))
        NotificationType.COMMENT -> NotificationIconData(Icons.Default.Comment, Color(0xFF2196F3), Color(0xFF2196F3).copy(alpha = 0.15f))
        NotificationType.REPLY -> NotificationIconData(Icons.Default.Reply, Color(0xFF9C27B0), Color(0xFF9C27B0).copy(alpha = 0.15f))
        NotificationType.FRIEND_REQUEST -> NotificationIconData(Icons.Default.PersonAdd, Color(0xFF4CAF50), Color(0xFF4CAF50).copy(alpha = 0.15f))
        NotificationType.SYSTEM -> NotificationIconData(Icons.Default.Warning, Color(red = 255, 0, 0), Color(red = 255, 0, 0).copy(alpha = 0.15f))
    }
}

fun getLocalizedMessage(notification: NotificationItem, preferencesManager: PreferencesManager): String {
    return when (notification.type) {
        NotificationType.LIKE -> preferencesManager.getString("notif_liked_post")
        NotificationType.COMMENT -> "${preferencesManager.getString("notif_commented")}: '${notification.message.substringAfter(": '").substringBefore("'")}'"
        NotificationType.REPLY -> preferencesManager.getString("notif_replied")
        NotificationType.FRIEND_REQUEST -> preferencesManager.getString("notif_friend_request")
        NotificationType.SYSTEM -> notification.message
    }
}