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
        startDestination = Screen.ProfileHome.route
    ) {
        // Profile Home Screen
        composable(Screen.ProfileHome.route) {
            ProfileScreen(
                preferencesManager = preferencesManager,
                onNavigateToAccountSettings = {
                    profileNavController.navigate(Screen.AccountSettings.route)
                },
                onNavigateToAccountData = {
                    profileNavController.navigate(Screen.AccountData.route)
                },
                onNavigateToHelpSupport = {
                    profileNavController.navigate(Screen.HelpSupport.route)
                },
                onNavigateToSettings = {
                    profileNavController.navigate(Screen.Settings.route)
                },
                onLogout = onLogout
            )
        }

        // Account Settings Screen
        composable(Screen.AccountSettings.route) {
            AccountSettingsScreen(
                preferencesManager = preferencesManager,
                onBackClick = {
                    profileNavController.popBackStack()
                }
            )
        }

        // Account Data Screen
        composable(Screen.AccountData.route) {
            AccountDataScreen(
                preferencesManager = preferencesManager,
                onBackClick = {
                    profileNavController.popBackStack()
                }
            )
        }

        // Help & Support Screen
        composable(Screen.HelpSupport.route) {
            HelpSupportScreen(
                onBackClick = {
                    profileNavController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
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