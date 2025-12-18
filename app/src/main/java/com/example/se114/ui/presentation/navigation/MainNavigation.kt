package com.example.se114.ui.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.chat.ChatNavGraph
import com.example.se114.ui.presentation.emergency.EmergencyScreen
import com.example.se114.ui.presentation.home.HomeScreen
import com.example.se114.ui.presentation.notification.NotificationScreen
import com.example.se114.ui.presentation.other_profile.OtherProfileScreen
import com.example.se114.ui.presentation.saved.SavedScreen

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
                preferencesManager = preferencesManager,
                onNavigateToNotification = {
                    navController.navigate("notification")
                },
                onNavigateToOtherProfile = { userId ->
                    navController.navigate(Screen.OtherProfile.createRoute(userId))
                }
            )
        }
        composable(BottomNavItem.Saved.route) {
            SavedScreen()
        }
        composable(BottomNavItem.Emergency.route) {
            EmergencyScreen()
        }
        // Đặc biệt để ẩn thanh bottom bar
        ChatNavGraph(
            navController,
            preferencesManager
        )
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

        // Màn hình Profile người khác
        composable(
            route = Screen.OtherProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            OtherProfileScreen(
                preferencesManager = preferencesManager,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}