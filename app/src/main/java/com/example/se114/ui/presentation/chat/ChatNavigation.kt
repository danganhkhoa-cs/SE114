package com.example.se114.ui.presentation.chat

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.navigation.BottomNavItem
import com.example.se114.ui.presentation.navigation.Screen

fun NavGraphBuilder.ChatNavGraph(
    navController: NavController,
    preferencesManager: PreferencesManager
) {
    navigation(
        route = BottomNavItem.Chat.route,
        startDestination = "chat_list"
    ) {

        composable(route = "chat_list") {
            ChatListScreen(
                onConversationClick = { conversationId ->
                    navController.navigate("chat_detail/$conversationId")
                },
                onSpamClick = {
                    navController.navigate("chat_spam")
                },
                onUserClick = { userId ->
                    // Navigate sang màn hình Other Profile
                    navController.navigate(Screen.OtherProfile.createRoute(userId))
                },
                preferencesManager = preferencesManager
            )
        }

        composable(route = "chat_spam") {
            ChatSpamScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onConversationClick = { conversationId ->
                    navController.navigate("chat_detail/$conversationId")
                },
                preferencesManager = preferencesManager
            )
        }

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
                onUserClick = { userId ->
                    // Navigate từ Chat Detail sang Other Profile
                    navController.navigate(Screen.OtherProfile.createRoute(userId))
                },
                preferencesManager = preferencesManager
            )
        }
    }
}