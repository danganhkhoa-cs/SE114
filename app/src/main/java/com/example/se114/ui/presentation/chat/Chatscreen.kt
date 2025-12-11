package com.example.se114.ui.presentation.chat

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.se114.local.PreferencesManager

@Composable
fun ChatScreen(
    preferencesManager: PreferencesManager // Thêm tham số này
) {
    val chatNavController = rememberNavController()

    NavHost(
        navController = chatNavController,
        startDestination = "chat_list"
    ) {
        composable("chat_list") {
            ChatListScreen(
                onConversationClick = { conversationId ->
                    chatNavController.navigate("chat_detail/$conversationId")
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
                    chatNavController.popBackStack()
                },
                preferencesManager = preferencesManager
            )
        }
    }
}