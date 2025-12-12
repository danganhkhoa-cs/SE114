package com.example.se114.ui.presentation.chat

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.navigation.BottomNavItem

fun NavGraphBuilder.ChatNavGraph(
    navController: NavController,
    preferencesManager: PreferencesManager
) {
    // Tạo một nested graph với route cha là "chat_graph"
    // startDestination là màn hình danh sách
    navigation(
        route = BottomNavItem.Chat.route,
        startDestination = "chat_list"
    ) {

        // 1. Màn hình danh sách Chat (Chat List)
        composable(route = "chat_list") {
            ChatListScreen(
                onConversationClick = { conversationId ->
                    // Điều hướng sang chi tiết, MainScreen sẽ tự động ẩn BottomBar
                    // nếu logic MainScreen check theo currentDestination
                    navController.navigate("chat_detail/$conversationId")
                },
                preferencesManager = preferencesManager
            )
        }

        // 2. Màn hình chi tiết Chat (Chat Detail)
        composable(
            route = "chat_detail/{conversationId}",
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("conversationId") ?: ""
            ChatDetailScreen(
                conversationId = id,
                onBackClick = {
                    navController.popBackStack()
                },
                preferencesManager = preferencesManager
            )
        }
    }
}