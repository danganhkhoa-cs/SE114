package com.example.se114

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.se114.data.local.PreferencesManager
import com.example.se114.ui.presentation.forgot_password.ForgotPasswordScreen
import com.example.se114.ui.presentation.forgot_password.OTPVerificationScreen
import com.example.se114.ui.presentation.login.LoginScreen
import com.example.se114.ui.presentation.main.MainScreen
import com.example.se114.ui.presentation.navigation.Screen
import com.example.se114.ui.presentation.register.RegisterScreen
import com.example.se114.ui.theme.SE114Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesManager = PreferencesManager(this)

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            var isDarkTheme by remember { mutableStateOf(preferencesManager.isDarkMode) }

            NavHost(
                navController = navController,
                startDestination = Screen.Login.route
            ) {
                // Login/Register/ForgotPassword - LUÔN SÁNG
                composable(route = Screen.Login.route) {
                    SE114Theme(darkTheme = false) {  // Force light mode
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            LoginScreen(
                                onLoginSuccess = {
                                    isDarkTheme = preferencesManager.isDarkMode
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
                    }
                }

                composable(route = Screen.Register.route) {
                    SE114Theme(darkTheme = false) {  // Force light mode
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
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
                    }
                }

                composable(route = Screen.ForgotPassword.route) {
                    SE114Theme(darkTheme = false) {  // Force light mode
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            ForgotPasswordScreen(
                                onSendMailSuccess = {
                                    navController.navigate(Screen.OTPVerification.route) {
                                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                                    }
                                },
                                onBackToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }

                composable(route = Screen.OTPVerification.route) {
                    SE114Theme(darkTheme = false) {  // Force light mode
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
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
                    }
                }

                // Main Screen - Dùng theme setting
                composable(route = Screen.Main.route) {
                    SE114Theme(darkTheme = isDarkTheme) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            MainScreen(
                                preferencesManager = preferencesManager,
                                isDarkTheme = isDarkTheme,
                                onThemeChange = { newTheme ->
                                    isDarkTheme = newTheme
                                    preferencesManager.isDarkMode = newTheme
                                },
                                onLogout = {
                                    preferencesManager.clearUserData()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}