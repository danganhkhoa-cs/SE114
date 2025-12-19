package com.example.se114.ui.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isEmergency: Boolean = false
) {
    object Home : BottomNavItem(
        route = "home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    // Đã sửa từ Rank thành Saved, và đổi Icon thành Bookmark
    object Saved : BottomNavItem(
        route = "saved",
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder
    )

    object Emergency : BottomNavItem(
        route = "emergency",
        selectedIcon = Icons.Filled.Warning,
        unselectedIcon = Icons.Filled.Warning,
        isEmergency = true
    )

    object Chat : BottomNavItem(
        route = "chat",
        selectedIcon = Icons.AutoMirrored.Filled.Chat,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline
    )

    object Profile : BottomNavItem(
        route = "profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.PersonOutline
    )
}