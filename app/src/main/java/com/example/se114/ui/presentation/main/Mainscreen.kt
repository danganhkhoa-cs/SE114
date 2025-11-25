package com.example.se114.ui.presentation.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.se114.ui.presentation.chat.ChatScreen
import com.example.se114.ui.presentation.emergency.EmergencyScreen
import com.example.se114.ui.presentation.home.HomeScreen
import com.example.se114.ui.presentation.navigation.BottomNavItem
import com.example.se114.ui.presentation.profile.ProfileScreen
import com.example.se114.ui.presentation.rank.RankScreen
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.AppTealLight

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Rank,
        BottomNavItem.Emergency,
        BottomNavItem.Chat,
        BottomNavItem.Profile
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Content
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen()
            }
            composable(BottomNavItem.Rank.route) {
                RankScreen()
            }
            composable(BottomNavItem.Emergency.route) {
                EmergencyScreen()
            }
            composable(BottomNavItem.Chat.route) {
                ChatScreen()
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen()
            }
        }

        // Bottom Navigation Bar - overlay on top with navigationBarsPadding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding() // ← KEY FIX: Thêm padding để tránh bị che bởi system navigation bar
                .height(95.dp)
        ) {
            // Main Bottom Bar with border and shadow
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(85.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        spotColor = Color(0xFF000000).copy(alpha = 0.2f),
                        ambientColor = Color(0xFF000000).copy(alpha = 0.15f),
                        clip = true
                    )
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = AppTealLight.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                border = BorderStroke(
                    width = 2.dp,
                    color = AppTealDark.copy(alpha = 0.2f)
                )
            ) {
                // Gradient overlay for depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
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
                        items.forEachIndexed { index, item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true

                            if (!item.isEmergency) {
                                // Regular navigation items
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
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
                                                    color = if (selected) AppTealDark.copy(alpha = 0.2f) else Color.Transparent,
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.title,
                                                modifier = Modifier.size(32.dp),
                                                tint = if (selected) AppTealDark else Color(0xFF757575)
                                            )
                                        }
                                    }

                                    // Selected indicator dot
                                    if (selected) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(
                                                    AppTealDark,
                                                    shape = CircleShape
                                                )
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(7.dp))
                                    }
                                }
                            } else {
                                // Spacer for emergency button
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Emergency FAB
            FloatingActionButton(
                onClick = {
                    navController.navigate(BottomNavItem.Emergency.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                containerColor = Color(0xFFE53935),
                contentColor = Color.White,
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 8.dp),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 18.dp,
                    pressedElevation = 22.dp,
                    hoveredElevation = 20.dp
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                    Icon(
                        imageVector = BottomNavItem.Emergency.selectedIcon,
                        contentDescription = "Emergency",
                        modifier = Modifier.size(38.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}