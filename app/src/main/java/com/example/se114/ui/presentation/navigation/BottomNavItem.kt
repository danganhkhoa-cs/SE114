package com.example.se114.ui.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isEmergency: Boolean = false
) {
    object Home : BottomNavItem(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    // Đã sửa từ Rank thành Saved, và đổi Icon thành Bookmark
    object Saved : BottomNavItem(
        route = "saved",
        title = "Saved",
        selectedIcon = Icons.Filled.Bookmark,       // Hình đặc khi được chọn
        unselectedIcon = Icons.Outlined.BookmarkBorder // Hình viền rỗng như bạn yêu cầu
    )

    object Emergency : BottomNavItem(
        route = "emergency",
        title = "Emergency",
        selectedIcon = Icons.Filled.Warning,
        unselectedIcon = Icons.Filled.Warning,
        isEmergency = true
    )

    object Chat : BottomNavItem(
        route = "chat",
        title = "Chat",
        selectedIcon = Icons.AutoMirrored.Filled.Chat,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline
    )

    object Profile : BottomNavItem(
        route = "profile",
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline
    )
}