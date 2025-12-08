package com.example.se114.ui.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.chat.ChatScreen
import com.example.se114.ui.presentation.emergency.EmergencyScreen
import com.example.se114.ui.presentation.home.HomeScreen
import com.example.se114.ui.presentation.notification.NotificationScreen
import com.example.se114.ui.presentation.rank.RankScreen

@Composable
fun MainNavGraph(
    navController: NavHostController,
    preferencesManager: PreferencesManager,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen(
                onNavigateToNotification = {
                    navController.navigate("notification")
                }
            )
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
            ProfileNavGraph(
                preferencesManager = preferencesManager,
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                onLogout = onLogout
            )
        }

        // MERGED FEATURE: Màn hình Notification được thêm vào NavGraph chính
        composable("notification") {
            NotificationScreen(
                onBackClick = {
                    navController.navigateUp()
                },
                preferencesManager = preferencesManager
            )
        }
    }
}