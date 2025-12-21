package com.example.se114.ui.presentation.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.chat.ChatListViewModel
import com.example.se114.ui.presentation.navigation.BottomNavItem
import com.example.se114.ui.presentation.navigation.MainNavGraph
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.AppTealLight
import com.example.se114.ui.theme.AppTealNeon
import com.example.se114.ui.theme.DarkSurface

@Composable
fun MainScreen(
    preferencesManager: PreferencesManager,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Inject ChatListViewModel để lấy số lượng tin nhắn chưa đọc
    // Lưu ý: ViewModel này sẽ sống cùng scope với MainScreen (là toàn bộ Activity)
    // Nếu bạn muốn scope nhỏ hơn thì cần điều chỉnh Graph, nhưng để làm Badge toàn cục thì ở đây là hợp lý.
    val chatViewModel: ChatListViewModel = hiltViewModel()
    val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val unreadCount = chatUiState.totalUnreadCount

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Saved,
        BottomNavItem.Emergency,
        BottomNavItem.Chat,
        BottomNavItem.Profile
    )

    val isFullScreen = currentDestination?.route == "notification"
            || currentDestination?.route?.contains("chat_detail") == true
            || currentDestination?.route?.contains("other_profile") == true

    // Cấu hình màu cho Bottom Bar "Bóng bẩy"
    val bottomBarBaseColor = if (isDarkTheme) {
        DarkSurface.copy(alpha = 0.9f)
    } else {
        AppTealLight.copy(alpha = 0.95f)
    }

    val bottomBarBorderColor = if (isDarkTheme) {
        AppTealNeon.copy(alpha = 0.3f)
    } else {
        AppTealDark.copy(alpha = 0.2f)
    }

    val fabColor = if (isDarkTheme) AppTealNeon else AppTealDark
    val fabContentColor = if (isDarkTheme) Color(0xFF00363D) else Color.White

    Scaffold(
        bottomBar = {
            if (!isFullScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(85.dp)
                            .shadow(
                                elevation = if (isDarkTheme) 32.dp else 24.dp,
                                spotColor = if (isDarkTheme) AppTealNeon.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.4f),
                                ambientColor = if (isDarkTheme) AppTealNeon.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.3f),
                                clip = true
                            )
                            .align(Alignment.BottomCenter),
                        color = bottomBarBaseColor,
                        tonalElevation = 0.dp,
                        border = BorderStroke(width = 1.dp, color = bottomBarBorderColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = if (isDarkTheme) {
                                            listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f), Color.Transparent)
                                        } else {
                                            listOf(Color.White.copy(alpha = 0.6f), Color.Transparent)
                                        }
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 24.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items.forEachIndexed { _, item ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                                    if (!item.isEmergency) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box {
                                                IconButton(
                                                    onClick = {
                                                        navController.navigate(item.route) {
                                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    },
                                                    modifier = Modifier.size(64.dp)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(64.dp)
                                                            .background(
                                                                color = if (selected) {
                                                                    if (isDarkTheme) AppTealNeon.copy(alpha = 0.15f)
                                                                    else AppTealDark.copy(alpha = 0.2f)
                                                                } else {
                                                                    Color.Transparent
                                                                },
                                                                shape = RoundedCornerShape(18.dp)
                                                            )
                                                    ) {
                                                        Icon(
                                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                            contentDescription = "",
                                                            modifier = Modifier.size(32.dp),
                                                            tint = if (selected) {
                                                                if (isDarkTheme) AppTealNeon else AppTealDark
                                                            } else {
                                                                if (isDarkTheme) Color(0xFF90A4AE) else Color(0xFF757575)
                                                            }
                                                        )
                                                    }
                                                }

                                                // --- HIỂN THỊ BADGE UNREAD COUNT ---
                                                if (item == BottomNavItem.Chat && unreadCount > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(x = (-8).dp, y = 8.dp)
                                                            .size(20.dp)
                                                            .background(Color(0xFFFF1744), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            if (selected) {
                                                Box(modifier = Modifier.size(7.dp).background(if (isDarkTheme) AppTealNeon else AppTealDark, shape = CircleShape))
                                            } else {
                                                Spacer(modifier = Modifier.height(7.dp))
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            navController.navigate(BottomNavItem.Emergency.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        containerColor = fabColor,
                        contentColor = fabContentColor,
                        modifier = Modifier.size(70.dp).align(Alignment.TopCenter).offset(y = 8.dp),
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 18.dp, pressedElevation = 22.dp, hoveredElevation = 20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.size(64.dp).background(Color.White.copy(alpha = 0.1f), shape = CircleShape))
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Create Post", modifier = Modifier.size(38.dp), tint = fabContentColor)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        MainNavGraph(
            navController = navController,
            preferencesManager = preferencesManager,
            isDarkTheme = isDarkTheme,
            onThemeChange = onThemeChange,
            onLogout = onLogout,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )
    }
}