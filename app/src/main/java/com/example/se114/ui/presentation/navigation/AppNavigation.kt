package com.example.se114.ui.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.se114.data.local.PreferencesManager
import com.example.se114.ui.presentation.forgot_password.ForgotPasswordScreen
import com.example.se114.ui.presentation.forgot_password.OTPVerificationScreen
import com.example.se114.ui.presentation.login.LoginScreen
import com.example.se114.ui.presentation.main.MainScreen
import com.example.se114.ui.presentation.register.RegisterScreen

@Composable
fun AppNavigation(
    preferencesManager: PreferencesManager,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {

        // Login Screen
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        // Register Screen
        composable(route = Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Forgot Password Screen
        composable(route = Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onSendMailSuccess = {
                    navController.navigate(Screen.OTPVerification.route) {
                        popUpTo(Screen.ForgotPassword.route) {inclusive = true}
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // OTP Verification Screen
        composable(route = Screen.OTPVerification.route) {
            OTPVerificationScreen(
                onResetPasswordSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        // Main Screen with Bottom Navigation
        composable(route = Screen.Main.route) {
            MainScreen(
                preferencesManager = preferencesManager,
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                onLogout = {
                    // Clear user data but keep settings
                    preferencesManager.clearUserData()

                    // Navigate back to login and clear all back stack
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
    }
}