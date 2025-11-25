package com.example.se114.ui.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.se114.data.local.PreferencesManager
import com.example.se114.ui.presentation.profile.*

@Composable
fun ProfileNavGraph(
    preferencesManager: PreferencesManager,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val profileNavController = rememberNavController()

    NavHost(
        navController = profileNavController,
        startDestination = "profile_home"
    ) {
        // Profile Home Screen
        composable("profile_home") {
            ProfileScreen(
                preferencesManager = preferencesManager,
                onNavigateToAccountSettings = {
                    profileNavController.navigate("account_settings")
                },
                onNavigateToAccountData = {
                    profileNavController.navigate("account_data")
                },
                onNavigateToHelpSupport = {
                    profileNavController.navigate("help_support")
                },
                onNavigateToSettings = {
                    profileNavController.navigate("settings")
                },
                onLogout = onLogout
            )
        }

        // Account Settings Screen
        composable("account_settings") {
            AccountSettingsScreen(
                preferencesManager = preferencesManager,
                onBackClick = {
                    profileNavController.popBackStack()
                }
            )
        }

        // Account Data Screen
        composable("account_data") {
            AccountDataScreen(
                preferencesManager = preferencesManager,
                onBackClick = {
                    profileNavController.popBackStack()
                }
            )
        }

        // Help & Support Screen
        composable("help_support") {
            HelpSupportScreen(
                onBackClick = {
                    profileNavController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable("settings") {
            SettingsScreen(
                preferencesManager = preferencesManager,
                onBackClick = {
                    profileNavController.popBackStack()
                },
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange
            )
        }
    }
}